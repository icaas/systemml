/**
 * IBM Confidential
 * OCO Source Materials
 * (C) Copyright IBM Corp. 2010, 2015
 * The source code for this program is not published or otherwise divested of its trade secrets, irrespective of what has been deposited with the U.S. Copyright Office.
 */

package com.ibm.bi.dml.test.integration.functions.unary.matrix;

import java.util.HashMap;

import org.junit.Assert;
import org.junit.Test;

import com.ibm.bi.dml.api.DMLScript.RUNTIME_PLATFORM;
import com.ibm.bi.dml.hops.OptimizerUtils;
import com.ibm.bi.dml.lops.LopProperties.ExecType;
import com.ibm.bi.dml.runtime.matrix.data.MatrixValue.CellIndex;
import com.ibm.bi.dml.test.integration.AutomatedTestBase;
import com.ibm.bi.dml.test.integration.TestConfiguration;
import com.ibm.bi.dml.test.utils.TestUtils;
import com.ibm.bi.dml.utils.Statistics;

/**
 * 
 * 
 */
public class FullCummaxTest extends AutomatedTestBase 
{
	@SuppressWarnings("unused")
	private static final String _COPYRIGHT = "Licensed Materials - Property of IBM\n(C) Copyright IBM Corp. 2010, 2015\n" +
                                             "US Government Users Restricted Rights - Use, duplication  disclosure restricted by GSA ADP Schedule Contract with IBM Corp.";
	
	private final static String TEST_NAME = "Cummax";
	private final static String TEST_DIR = "functions/unary/matrix/";
	
	private final static double eps = 1e-10;
	
	private final static int rowsMatrix = 1201;
	private final static int colsMatrix = 1103;
	private final static double spSparse = 0.1;
	private final static double spDense = 0.9;
	
	private enum InputType {
		COL_VECTOR,
		ROW_VECTOR,
		MATRIX
	}
	
	@Override
	public void setUp() 
	{
		addTestConfiguration(TEST_NAME,new TestConfiguration(TEST_DIR, TEST_NAME,new String[]{"B"})); 
	}

	// -----------------------------------------------------------------
	
	@Test
	public void testCummaxColVectorDenseSP() 
	{
		if(rtplatform == RUNTIME_PLATFORM.SPARK)
		runColAggregateOperationTest(InputType.COL_VECTOR, false, ExecType.SPARK);
	}
	
	@Test
	public void testCummaxRowVectorDenseSP() 
	{
		if(rtplatform == RUNTIME_PLATFORM.SPARK)
		runColAggregateOperationTest(InputType.ROW_VECTOR, false, ExecType.SPARK);
	}
	
	@Test
	public void testCummaxRowVectorDenseNoRewritesSP() 
	{
		if(rtplatform == RUNTIME_PLATFORM.SPARK)
		runColAggregateOperationTest(InputType.ROW_VECTOR, false, ExecType.SPARK, false);
	}
	
	@Test
	public void testCummaxMatrixDenseSP() 
	{
		if(rtplatform == RUNTIME_PLATFORM.SPARK)
		runColAggregateOperationTest(InputType.MATRIX, false, ExecType.SPARK);
	}
	
	@Test
	public void testCummaxColVectorSparseSP() 
	{
		if(rtplatform == RUNTIME_PLATFORM.SPARK)
		runColAggregateOperationTest(InputType.COL_VECTOR, true, ExecType.SPARK);
	}
	
	@Test
	public void testCummaxRowVectorSparseSP() 
	{
		if(rtplatform == RUNTIME_PLATFORM.SPARK)
		runColAggregateOperationTest(InputType.ROW_VECTOR, true, ExecType.SPARK);
	}
	
	@Test
	public void testCummaxRowVectorSparseNoRewritesSP() 
	{
		if(rtplatform == RUNTIME_PLATFORM.SPARK)
		runColAggregateOperationTest(InputType.ROW_VECTOR, true, ExecType.SPARK, false);
	}
	
	@Test
	public void testCummaxMatrixSparseSP() 
	{
		if(rtplatform == RUNTIME_PLATFORM.SPARK)
		runColAggregateOperationTest(InputType.MATRIX, true, ExecType.SPARK);
	}
	
	
	// -----------------------------------------------------------------
	
	@Test
	public void testCummaxColVectorDenseCP() 
	{
		runColAggregateOperationTest(InputType.COL_VECTOR, false, ExecType.CP);
	}
	
	@Test
	public void testCummaxRowVectorDenseCP() 
	{
		runColAggregateOperationTest(InputType.ROW_VECTOR, false, ExecType.CP);
	}
	
	@Test
	public void testCummaxRowVectorDenseNoRewritesCP() 
	{
		runColAggregateOperationTest(InputType.ROW_VECTOR, false, ExecType.CP, false);
	}
	
	@Test
	public void testCummaxMatrixDenseCP() 
	{
		runColAggregateOperationTest(InputType.MATRIX, false, ExecType.CP);
	}
	
	@Test
	public void testCummaxColVectorSparseCP() 
	{
		runColAggregateOperationTest(InputType.COL_VECTOR, true, ExecType.CP);
	}
	
	@Test
	public void testCummaxRowVectorSparseCP() 
	{
		runColAggregateOperationTest(InputType.ROW_VECTOR, true, ExecType.CP);
	}
	
	@Test
	public void testCummaxRowVectorSparseNoRewritesCP() 
	{
		runColAggregateOperationTest(InputType.ROW_VECTOR, true, ExecType.CP, false);
	}
	
	@Test
	public void testCummaxMatrixSparseCP() 
	{
		runColAggregateOperationTest(InputType.MATRIX, true, ExecType.CP);
	}
	
	@Test
	public void testCummaxColVectorDenseMR() 
	{
		runColAggregateOperationTest(InputType.COL_VECTOR, false, ExecType.MR);
	}
	
	@Test
	public void testCummaxRowVectorDenseMR() 
	{
		runColAggregateOperationTest(InputType.ROW_VECTOR, false, ExecType.MR);
	}
	
	@Test
	public void testCummaxRowVectorDenseNoRewritesMR() 
	{
		runColAggregateOperationTest(InputType.ROW_VECTOR, false, ExecType.MR, false);
	}
	
	@Test
	public void testCummaxMatrixDenseMR() 
	{
		runColAggregateOperationTest(InputType.MATRIX, false, ExecType.MR);
	}
	
	@Test
	public void testCummaxColVectorSparseMR() 
	{
		runColAggregateOperationTest(InputType.COL_VECTOR, true, ExecType.MR);
	}
	
	@Test
	public void testCummaxRowVectorSparseNoRewritesMR() 
	{
		runColAggregateOperationTest(InputType.ROW_VECTOR, true, ExecType.MR, false);
	}
	
	@Test
	public void testCummaxMatrixSparseMR() 
	{
		runColAggregateOperationTest(InputType.MATRIX, true, ExecType.MR);
	}
	
	/**
	 * 
	 * @param type
	 * @param sparse
	 * @param instType
	 */
	private void runColAggregateOperationTest( InputType type, boolean sparse, ExecType instType)
	{
		//by default we apply algebraic simplification rewrites
		runColAggregateOperationTest(type, sparse, instType, true);
	}
	
	/**
	 * 
	 * @param sparseM1
	 * @param sparseM2
	 * @param instType
	 */
	private void runColAggregateOperationTest( InputType type, boolean sparse, ExecType instType, boolean rewrites)
	{
		//rtplatform for MR
		RUNTIME_PLATFORM platformOld = rtplatform;
		if(instType == ExecType.SPARK) {
	    	rtplatform = RUNTIME_PLATFORM.SPARK;
	    }
	    else {
	    	rtplatform = (instType==ExecType.MR) ? RUNTIME_PLATFORM.HADOOP : RUNTIME_PLATFORM.HYBRID;
	    }
		
		//rewrites
		boolean oldFlagRewrites = OptimizerUtils.ALLOW_ALGEBRAIC_SIMPLIFICATION;
		OptimizerUtils.ALLOW_ALGEBRAIC_SIMPLIFICATION = rewrites;
		
		try
		{
			int cols = (type==InputType.COL_VECTOR) ? 1 : colsMatrix;
			int rows = (type==InputType.ROW_VECTOR) ? 1 : rowsMatrix;
			double sparsity = (sparse) ? spSparse : spDense;
			
			TestConfiguration config = getTestConfiguration(TEST_NAME);
			
			// This is for running the junit test the new way, i.e., construct the arguments directly
			String HOME = SCRIPT_DIR + TEST_DIR;
			fullDMLScriptName = HOME + TEST_NAME + ".dml";
			programArgs = new String[]{"-explain", "-args", HOME + INPUT_DIR + "A",
					                        HOME + OUTPUT_DIR + "B"    };
			fullRScriptName = HOME + TEST_NAME + ".R";
			rCmd = "Rscript" + " " + fullRScriptName + " " + 
			       HOME + INPUT_DIR + " " + HOME + EXPECTED_DIR;
			
			loadTestConfiguration(config);
	
			//generate actual dataset 
			double[][] A = getRandomMatrix(rows, cols, -0.05, 1, sparsity, 7); 
			writeInputMatrixWithMTD("A", A, true);
	
			runTest(true, false, null, -1); 
			if( instType==ExecType.CP ) //in CP no MR jobs should be executed
				Assert.assertEquals("Unexpected number of executed MR jobs.", 0, Statistics.getNoOfExecutedMRJobs());
			
			runRScript(true); 
		
			//compare matrices 
			HashMap<CellIndex, Double> dmlfile = readDMLMatrixFromHDFS("B");
			HashMap<CellIndex, Double> rfile  = readRMatrixFromFS("B");
			TestUtils.compareMatrices(dmlfile, rfile, eps, "Stat-DML", "Stat-R");
		}
		finally
		{
			rtplatform = platformOld;
			OptimizerUtils.ALLOW_ALGEBRAIC_SIMPLIFICATION = oldFlagRewrites;
		}
	}	
}