/**
 * IBM Confidential
 * OCO Source Materials
 * (C) Copyright IBM Corp. 2010, 2014
 * The source code for this program is not published or otherwise divested of its trade secrets, irrespective of what has been deposited with the U.S. Copyright Office.
 */

package com.ibm.bi.dml.debugger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.Map.Entry;

import com.ibm.bi.dml.conf.DMLConfig;
import com.ibm.bi.dml.parser.DMLProgram;
import com.ibm.bi.dml.parser.DMLTranslator;
import com.ibm.bi.dml.runtime.controlprogram.ExternalFunctionProgramBlock;
import com.ibm.bi.dml.runtime.controlprogram.ForProgramBlock;
import com.ibm.bi.dml.runtime.controlprogram.FunctionProgramBlock;
import com.ibm.bi.dml.runtime.controlprogram.IfProgramBlock;
import com.ibm.bi.dml.runtime.controlprogram.Program;
import com.ibm.bi.dml.runtime.controlprogram.ProgramBlock;
import com.ibm.bi.dml.runtime.controlprogram.WhileProgramBlock;
import com.ibm.bi.dml.runtime.instructions.BPInstruction;
import com.ibm.bi.dml.runtime.instructions.Instruction;
import com.ibm.bi.dml.runtime.instructions.MRJobInstruction;
import com.ibm.bi.dml.runtime.instructions.BPInstruction.BPINSTRUCTION_STATUS;
import com.ibm.bi.dml.runtime.instructions.CPInstructions.CPInstruction;

/**
 * This class contains the parsed and compiled DML script w/ hops, lops and runtime program.
 * Additionally, it provides an interface for managing breakpoints and disassembling a DML script.  
 * Note: ONLY USED FOR DEBUGGING PURPOSES 
 */
public class DMLDebuggerProgramInfo 
{
	@SuppressWarnings("unused")
	private static final String _COPYRIGHT = "Licensed Materials - Property of IBM\n(C) Copyright IBM Corp. 2010, 2014\n" +
                                             "US Government Users Restricted Rights - Use, duplication  disclosure restricted by GSA ADP Schedule Contract with IBM Corp.";
	
	public DMLConfig conf; //DML configuration information
	public DMLProgram prog; //DML program representation 
	public DMLTranslator dmlt; //DML program hops and lops, rewrites
	public Program rtprog; //DML runtime program

	private TreeMap<Integer, ArrayList<Instruction>> disassembler; //map between DML program line numbers and corresponding runtime instruction(s)
	
	private int prevLineNum = 0; //used for approximating line numbers for instructions (if necessary)  
	private long instID = 1; //runtime instruction ID
	
	private String location=null; //DML program namespace and function name
	
	/**
	 * Constructor for DMLDebuggerProgramInfo class.
	 */
	public DMLDebuggerProgramInfo() {
		disassembler = new TreeMap<Integer, ArrayList<Instruction>>();
	}
	
	/** 
	 * Getter for DML script to instruction map field
	 * @return DMLInstMap Map between DML script line numbers and runtime instruction(s)
	 */
	public TreeMap<Integer, ArrayList<Instruction>> getDMLInstMap() {
		this.setDMLInstMap();
		return disassembler;
	}
			
	/**
	 * Access breakpoint instruction at specified line number in runtime program (if valid)
	 * @param lineNumber Location for breakpoint operation
	 * @param op Breakpoint operation (op=0 for create, op=1 for enable/disable, op=2 for delete)
	 * @param status Current breakpoint status   
	 */
	public void accessBreakpoint(int lineNumber, int op, BPINSTRUCTION_STATUS status)
	{		
		if (this.rtprog != null) 
		{
			//Functions: For each function program block (if any), get instructions corresponding to each line number
			HashMap<String,FunctionProgramBlock> funcMap = this.rtprog.getFunctionProgramBlocks();
			if (funcMap != null && funcMap.size() > 0) {
				for (Entry<String, FunctionProgramBlock> e : funcMap.entrySet()) 
				{
					location = e.getKey();
					FunctionProgramBlock fpb = e.getValue();
					if(fpb instanceof ExternalFunctionProgramBlock)
						continue;
					else 
					{						
						for (ProgramBlock pb : fpb.getChildBlocks())
							accessProgramBlockBreakpoint(pb, lineNumber, op, status);
					}
				}
			}
			//Main program: for each program block, get instructions corresponding to current line number (if any)
			location=DMLProgram.constructFunctionKey(DMLProgram.DEFAULT_NAMESPACE, "main");
			for (ProgramBlock pb : this.rtprog.getProgramBlocks()) 
			{
				if (pb != null)
					accessProgramBlockBreakpoint(pb, lineNumber, op, status);
			}
		}
	}
	
	/**
	 * Access breakpoint instruction at specified line number in program block (if valid)
	 * @param pb Current program block 
	 * @param lineNumber Location for inserting breakpoint
	 * @param status Current breakpoint status   
	 */
	private void accessProgramBlockBreakpoint(ProgramBlock pb, int lineNumber, int op, BPINSTRUCTION_STATUS status) 
	{		
		if (pb instanceof FunctionProgramBlock)
		{
			FunctionProgramBlock fpb = (FunctionProgramBlock)pb;
			for( ProgramBlock pbc : fpb.getChildBlocks() )
				accessProgramBlockBreakpoint(pbc, lineNumber, op, status);
		}
		else if (pb instanceof WhileProgramBlock)
		{
			WhileProgramBlock wpb = (WhileProgramBlock) pb;
			this.accesBreakpointInstruction(wpb.getPredicate(), lineNumber, op, status);
			for( ProgramBlock pbc : wpb.getChildBlocks() )
				accessProgramBlockBreakpoint(pbc, lineNumber, op, status);
		}	
		else if (pb instanceof IfProgramBlock)
		{
			IfProgramBlock ipb = (IfProgramBlock) pb;
			this.accesBreakpointInstruction(ipb.getPredicate(), lineNumber, op, status);
			for( ProgramBlock pbc : ipb.getChildBlocksIfBody() )
				accessProgramBlockBreakpoint(pbc, lineNumber, op, status);
			if( ipb.getChildBlocksElseBody().size()>0 )
			{	
				for( ProgramBlock pbc : ipb.getChildBlocksElseBody() ) 
					accessProgramBlockBreakpoint(pbc, lineNumber, op, status);
			}
		}
		else if (pb instanceof ForProgramBlock) //incl parfor
		{
			ForProgramBlock fpb = (ForProgramBlock) pb;
			this.accesBreakpointInstruction(fpb.getFromInstructions(), lineNumber, op, status);
			this.accesBreakpointInstruction(fpb.getToInstructions(), lineNumber, op, status);
			this.accesBreakpointInstruction(fpb.getIncrementInstructions(), lineNumber, op, status);
			for( ProgramBlock pbc : fpb.getChildBlocks() ) 
				accessProgramBlockBreakpoint(pbc, lineNumber, op, status);
		}
		else
		{
			this.accesBreakpointInstruction(pb.getInstructions(), lineNumber, op, status);
		}
	}

	/**
	 * Access breakpoint instruction at specified line number in set of instructions (if valid)
	 * @param instructions Instructions for current program block
	 * @param lineNumber Location for inserting breakpoint
	 * @param status Current breakpoint status
	 */
	private void accesBreakpointInstruction(ArrayList<Instruction> instructions, int lineNumber, int op, BPINSTRUCTION_STATUS status) 
	{
		for (int i = 0; i < instructions.size() ; i++) 
		{
			Instruction currInst = instructions.get(i);			
			if (op == 0)  
			{
				if (currInst instanceof MRJobInstruction)  
				{
					MRJobInstruction currMRInst = (MRJobInstruction) currInst;
					// Check if current instruction line number correspond to breakpoint line number
					if (currMRInst.findMRInstructions(lineNumber))  {						
						BPInstruction breakpoint = new BPInstruction();
						breakpoint.setLineNum(lineNumber);
						breakpoint.setInstID(instID++);
						breakpoint.setBPInstructionLocation(location);
						instructions.add(i, breakpoint);
						DMLBreakpointManager.insertBreakpoint(breakpoint, lineNumber); 						
						return;
					}
				}
				else if (currInst instanceof CPInstruction) 
				{
					// Check if current instruction line number correspond to breakpoint line number
					if (currInst.getLineNum() == lineNumber) {
						BPInstruction breakpoint = new BPInstruction();
						breakpoint.setLineNum(lineNumber);
						breakpoint.setInstID(instID++);
						breakpoint.setBPInstructionLocation(location);
						instructions.add(i, breakpoint);
						DMLBreakpointManager.insertBreakpoint(breakpoint, lineNumber); 
						return;
					}
				}
				else if (currInst instanceof BPInstruction && currInst.getLineNum() == lineNumber) 
				{
					BPInstruction breakpoint = (BPInstruction) currInst;
					breakpoint.setBPInstructionStatus(BPINSTRUCTION_STATUS.ENABLED);
					breakpoint.setBPInstructionLocation(location);
					instructions.set(i, breakpoint);
					DMLBreakpointManager.updateBreakpoint(lineNumber, status);
					return;
				}
			}
			else 
			{
				// Check if current instruction line number correspond to breakpoint line number
				if (currInst instanceof BPInstruction && currInst.getLineNum() == lineNumber) 
				{
					if (op == 1) 
					{
						BPInstruction breakpoint = (BPInstruction) currInst;
						breakpoint.setLineNum(currInst.getLineNum());
						breakpoint.setInstID(currInst.getInstID());
						breakpoint.setBPInstructionStatus(status);
						breakpoint.setBPInstructionLocation(location);
						instructions.set(i, breakpoint);
						DMLBreakpointManager.updateBreakpoint(lineNumber, status);
					}
					else 
					{
						instructions.remove(i);
						DMLBreakpointManager.removeBreakpoint(lineNumber, status);
					}
					return;
				}
			}
		}
	}
	
	/**
	 * Getter for DML instruction map field (i.e. disassembler functionality) 
	 * For each DML script line, get runtime instructions (if any)
	 */
	public void setDMLInstMap() 
	{
		disassembler = new TreeMap<Integer, ArrayList<Instruction>>();		
		if (this.rtprog != null)  
		{
			//Functions: For each function program block (if any), get instructions corresponding to each line number
			HashMap<String,FunctionProgramBlock> funcMap = this.rtprog.getFunctionProgramBlocks();
			if (funcMap != null && funcMap.size() > 0)
			{
				for (Entry<String, FunctionProgramBlock> e : funcMap.entrySet())
				{					
					FunctionProgramBlock fpb = e.getValue();
					if(fpb instanceof ExternalFunctionProgramBlock)
						continue;
					else
					{						
						for (ProgramBlock pb : fpb.getChildBlocks())
							setProgramBlockInstMap(pb);
					}
				}
			}
			//Main program: for each program block, get instructions corresponding to current line number (if any)
			for (ProgramBlock pb : this.rtprog.getProgramBlocks())  
			{
				if (pb != null)	
					setProgramBlockInstMap(pb);
			}
		}
	}
	
	/**
	 * For each program block, get runtime instructions (if any)
	 * @param pb Current program block 
	 */
	private void setProgramBlockInstMap(ProgramBlock pb) 
	{		
		if (pb instanceof FunctionProgramBlock)
		{
			FunctionProgramBlock fpb = (FunctionProgramBlock)pb;
			for( ProgramBlock pbc : fpb.getChildBlocks() )
				setProgramBlockInstMap(pbc);
		}
		else if (pb instanceof WhileProgramBlock)
		{
			WhileProgramBlock wpb = (WhileProgramBlock) pb;
			this.setInstMap(wpb.getPredicate());			
			for( ProgramBlock pbc : wpb.getChildBlocks() )
				setProgramBlockInstMap(pbc);				
		}	
		else if (pb instanceof IfProgramBlock)
		{
			IfProgramBlock ipb = (IfProgramBlock) pb;
			this.setInstMap(ipb.getPredicate());
			for( ProgramBlock pbc : ipb.getChildBlocksIfBody() )
				setProgramBlockInstMap(pbc);
			if( ipb.getChildBlocksElseBody().size()>0 )
			{	
				for( ProgramBlock pbc : ipb.getChildBlocksElseBody() ) 
					setProgramBlockInstMap(pbc);
			}
		}
		else if (pb instanceof ForProgramBlock) //incl parfor
		{
			ForProgramBlock fpb = (ForProgramBlock) pb;
			this.setInstMap(fpb.getFromInstructions());
			this.setInstMap(fpb.getToInstructions());
			this.setInstMap(fpb.getIncrementInstructions());
			for( ProgramBlock pbc : fpb.getChildBlocks() ) 
				setProgramBlockInstMap(pbc);
		}
		else
		{
			this.setInstMap(pb.getInstructions());
		}
	}

	/**
	 * For each instruction, generate map with corresponding DML 
	 * script line number 
	 * @param instructions Instructions for current program block 
	 */
	private void setInstMap(ArrayList<Instruction> instructions)
	{		
		for (int i = 0; i < instructions.size() ; i++)
		{
			Instruction currInst = instructions.get(i);
			//set instruction unique identifier
			if (currInst.getInstID() == 0) {				 
				currInst.setInstID(instID++);				
			}
			if (currInst instanceof MRJobInstruction)  
			{
				MRJobInstruction currMRInst = (MRJobInstruction) currInst;				
				int min = Integer.MAX_VALUE;
				//iterate of MR job instructions to identify minimum line number
				for (Integer lineNumber : currMRInst.getMRJobInstructionsLineNumbers())
				{
					if (lineNumber < min)
						min = lineNumber;
				}
				//set MR job line number
				if (min == 0 || min == Integer.MAX_VALUE)
					currMRInst.setLineNum(prevLineNum); //last seen instruction line number
				else
					currMRInst.setLineNum(min); //minimum instruction line number for this MR job
				//insert current MR instruction into corresponding source code line
				if (!disassembler.containsKey(currMRInst.getLineNum()))
					disassembler.put(currMRInst.getLineNum(), new ArrayList<Instruction>());
				disassembler.get(currMRInst.getLineNum()).add(currMRInst);
			}
			else if (currInst instanceof CPInstruction)
			{
				CPInstruction currCPInst = (CPInstruction) currInst;
				//if CP instruction line number is not set, then approximate to last seen line number
				if (currCPInst.getLineNum() == 0)
					currCPInst.setLineNum(prevLineNum);
				//insert current CP instruction into corresponding source code line
				if (!disassembler.containsKey(currCPInst.getLineNum()))
					disassembler.put(currCPInst.getLineNum(), new ArrayList<Instruction>());
				disassembler.get(currCPInst.getLineNum()).add(currCPInst);
			}
			else if (currInst instanceof BPInstruction)
			{
				BPInstruction currBPInst = (BPInstruction) currInst;
				//insert current BP instruction into corresponding source code line
				if (!disassembler.containsKey(currBPInst.getLineNum()))
					disassembler.put(currBPInst.getLineNum(), new ArrayList<Instruction>());
				disassembler.get(currInst.getLineNum()).add(currBPInst);
			}
			//save instruction's line number as last seen
			if (currInst.getLineNum() != 0)
				prevLineNum = currInst.getLineNum();
		}
	}
}
