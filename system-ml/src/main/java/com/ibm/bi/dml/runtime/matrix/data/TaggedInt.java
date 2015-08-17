/**
 * IBM Confidential
 * OCO Source Materials
 * (C) Copyright IBM Corp. 2010, 2015
 * The source code for this program is not published or otherwise divested of its trade secrets, irrespective of what has been deposited with the U.S. Copyright Office.
 */


package com.ibm.bi.dml.runtime.matrix.data;

import org.apache.hadoop.io.IntWritable;

public class TaggedInt extends Tagged<IntWritable>
{
	@SuppressWarnings("unused")
	private static final String _COPYRIGHT = "Licensed Materials - Property of IBM\n(C) Copyright IBM Corp. 2010, 2015\n" +
                                             "US Government Users Restricted Rights - Use, duplication  disclosure restricted by GSA ADP Schedule Contract with IBM Corp.";
	
	public TaggedInt()
	{
		tag=-1;
		base=new IntWritable();
	}

	public TaggedInt(IntWritable b, byte t) {
		super(b, t);
	}
	
	public int hashCode()
	{
		return base.hashCode()+tag;
	}
	
	public int compareTo(TaggedInt other)
	{
		if(this.tag!=other.tag)
			return (this.tag-other.tag);
		else if(this.base.get()!=other.base.get())
			return (this.base.get()-other.base.get());
		return 0;
	}

	@Override
	public boolean equals(Object other)
	{
		if( !(other instanceof TaggedInt))
			return false;
		
		TaggedInt tother = (TaggedInt)other;
		return (this.tag==tother.tag && this.base.get()==tother.base.get());
	}
}