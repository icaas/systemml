package com.ibm.bi.dml.hops;

import com.ibm.bi.dml.lops.Binary;
import com.ibm.bi.dml.lops.Group;
import com.ibm.bi.dml.lops.Lops;
import com.ibm.bi.dml.lops.RangeBasedReIndex;
import com.ibm.bi.dml.lops.ZeroOut;
import com.ibm.bi.dml.lops.LopProperties.ExecType;
import com.ibm.bi.dml.parser.Expression.DataType;
import com.ibm.bi.dml.parser.Expression.ValueType;
import com.ibm.bi.dml.sql.sqllops.SQLLops;
import com.ibm.bi.dml.utils.HopsException;

public class LeftIndexingOp  extends Hops {

	public LeftIndexingOp(String l, DataType dt, ValueType vt, Hops inpMatrixLeft, Hops inpMatrixRight, Hops inpRowL, Hops inpRowU, Hops inpColL, Hops inpColU) {
		super(Kind.Indexing, l, dt, vt);
		/*
		if(inpRowL==null)
			inpRowL=new DataOp("1", DataType.SCALAR, ValueType.INT, DataOpTypes.PERSISTENTREAD, "1", -1, -1, -1, -1);
		if(inpRowU==null)
			inpRowU=new DataOp(Long.toString(get_dim1()), DataType.SCALAR, ValueType.INT, DataOpTypes.PERSISTENTREAD, Long.toString(get_dim1()), -1, -1, -1, -1);
		if(inpColL==null)
			inpColL=new DataOp("1", DataType.SCALAR, ValueType.INT, DataOpTypes.PERSISTENTREAD, "1", -1, -1, -1, -1);
		if(inpColU==null)
			inpColU=new DataOp(Long.toString(get_dim2()), DataType.SCALAR, ValueType.INT, DataOpTypes.PERSISTENTREAD, Long.toString(get_dim2()), -1, -1, -1, -1);
	*/
		getInput().add(0, inpMatrixLeft);
		getInput().add(1, inpMatrixRight);
		getInput().add(2, inpRowL);
		getInput().add(3, inpRowU);
		getInput().add(4, inpColL);
		getInput().add(5, inpColU);
		
		// create hops if one of them is null
		inpMatrixLeft.getParent().add(this);
		inpMatrixRight.getParent().add(this);
		inpRowL.getParent().add(this);
		inpRowU.getParent().add(this);
		inpColL.getParent().add(this);
		inpColU.getParent().add(this);

	}

	public Lops constructLops()
			throws HopsException {
		if (get_lops() == null) {
			try {
				ExecType et = optFindExecType();
				if(et == ExecType.MR) {
					
					//the right matrix is reindexed
					Lops top=getInput().get(2).constructLops();
					Lops bottom=getInput().get(3).constructLops();
					Lops left=getInput().get(4).constructLops();
					Lops right=getInput().get(5).constructLops();
					/*
					//need to creat new lops for converting the index ranges
					//original range is (a, b) --> (c, d)
					//newa=2-a, newb=2-b
					Lops two=new Data(null,	Data.OperationTypes.READ, null, "2", Expression.DataType.SCALAR, Expression.ValueType.INT, false);
					Lops newTop=new Binary(two, top, HopsOpOp2LopsB.get(Hops.OpOp2.MINUS), Expression.DataType.SCALAR, Expression.ValueType.INT, et);
					Lops newLeft=new Binary(two, left, HopsOpOp2LopsB.get(Hops.OpOp2.MINUS), Expression.DataType.SCALAR, Expression.ValueType.INT, et);
					//newc=leftmatrix.row-a+1, newd=leftmatrix.row
					*/
					//right hand matrix
					RangeBasedReIndex reindex = new RangeBasedReIndex(
							getInput().get(1).constructLops(), top, bottom, 
							left, right, getInput().get(0).get_dim1(), getInput().get(0).get_dim2(),
							get_dataType(), get_valueType(), et, true);
					
					reindex.getOutputParameters().setDimensions(getInput().get(0).get_dim1(), getInput().get(0).get_dim2(), 
							get_rows_in_block(), get_cols_in_block(), getNnz());
					
					Group group1 = new Group(
							reindex, Group.OperationTypes.Sort, DataType.MATRIX,
							get_valueType());
					group1.getOutputParameters().setDimensions(getInput().get(0).get_dim1(), getInput().get(0).get_dim2(), 
							get_rows_in_block(), get_cols_in_block(), getNnz());
	
					//the left matrix is zeroed out
					ZeroOut zeroout = new ZeroOut(
							getInput().get(0).constructLops(), top, bottom,
							left, right, getInput().get(0).get_dim1(), getInput().get(0).get_dim2(),
							get_dataType(), get_valueType(), et);
	
					zeroout.getOutputParameters().setDimensions(getInput().get(0).get_dim1(), getInput().get(0).get_dim2(), 
							get_rows_in_block(), get_cols_in_block(), getNnz());
					Group group2 = new Group(
							zeroout, Group.OperationTypes.Sort, DataType.MATRIX,
							get_valueType());
					group2.getOutputParameters().setDimensions(getInput().get(0).get_dim1(), getInput().get(0).get_dim2(), 
							get_rows_in_block(), get_cols_in_block(), getNnz());
					
					Binary binary = new Binary(group1, group2, HopsOpOp2LopsB.get(Hops.OpOp2.PLUS),
							get_dataType(), get_valueType(), et);
					
					binary.getOutputParameters().setDimensions(getInput().get(0).get_dim1(), getInput().get(0).get_dim2(), 
							get_rows_in_block(), get_cols_in_block(), getNnz());
	
					set_lops(binary);
				}
				else {
					//TODO: how to implement leftIndexing in CP
					throw new HopsException("leftIndexing is not supported in CP yet!");
				}
			} catch (Exception e) {
				throw new HopsException(e);
			}

		}
		return get_lops();
	}

	@Override
	public String getOpString() {
		String s = new String("");
		s += "LeftIndexing";
		return s;
	}

	public void printMe() throws HopsException {
		if (get_visited() != VISIT_STATUS.DONE) {
			super.printMe();
			for (Hops h : getInput()) {
				h.printMe();
			}
			;
		}
		set_visited(VISIT_STATUS.DONE);
	}

	public SQLLops constructSQLLOPs() throws HopsException {
		throw new HopsException("LeftIndexingOp.constructSQLLOPs shoule not be called");
	}
	
	@Override
	public boolean allowsAllExecTypes()
	{
		return false;
	}
	
	@Override
	protected ExecType optFindExecType() throws HopsException {
//		if ( DMLScript.rtplatform == RUNTIME_PLATFORM.SINGLE_NODE )
//			return ExecType.CP;
//		
//		if( _etype != null ) 			
//			return _etype;
//		
//		if ( getInput().get(0).areDimsBelowThreshold() )
//			return ExecType.CP;
//		
		return ExecType.MR;
	}

}
