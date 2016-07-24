/**
 * Copyright (c) 2003-2010, Xith3D Project Group all rights reserved.
 * 
 * Portions based on the Java3D interface, Copyright by Sun Microsystems.
 * Many thanks to the developers of Java3D and Sun Microsystems for their
 * innovation and design.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * 
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * 
 * Neither the name of the 'Xith3D Project Group' nor the names of its
 * contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) A
 * RISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE
 */
package org.xith3d.scenegraph;

import org.jagatoo.opengl.enums.GeometryArrayType;

/**
 * @author David Yazel
 * @author Marvin Froehlich (aka Qudus)
 */
public abstract class IndexedGeometryArray extends Geometry
{
    private int[] index;
    private boolean indexDirty = true;
    
    /**
     * {@inheritDoc}
     */
    public final int getIndexCount()
    {
        return ( getIndex().length );
    }
    
    public final void setValidIndexCount( int count )
    {
        dataContainer.setValidVertexCount( count );
        
        setChanged( true );
        setBoundsDirty();
    }
    
    public final int getValidIndexCount()
    {
        return ( dataContainer.getValidVertexCount() );
    }
    
    public final void setInitialIndexIndex( int i )
    {
        dataContainer.setInitialIndex( i );
    }
    
    public final int getInitialIndexIndex()
    {
        return ( dataContainer.getInitialIndex() );
    }
    
    public final void setIndex( int[] indices )
    {
        System.arraycopy( indices, 0, index, 0, indices.length );
        indexDirty = true;
    }
    
    public final void setIndex( int i, int idx )
    {
        index[ i ] = idx;
        indexDirty = true;
    }
    
    /**
     * {@inheritDoc}
     */
    public final int getIndex( int i )
    {
        return ( index[ i ] );
    }
    
    /**
     * {@inheritDoc}
     */
    public final int[] getIndex()
    {
        return ( index );
    }
    
    public final GeomNioIntData getIndexData()
    {
        if ( indexDirty )
        {
            dataContainer.getIndexData().set( getInitialIndexIndex(), index, getInitialIndexIndex(), getValidIndexCount() );
            indexDirty = false;
        }
        
        return ( (GeomNioIntData)dataContainer.getIndexData() );
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void duplicateNodeComponent( NodeComponent original, boolean forceDuplicate )
    {
        super.duplicateNodeComponent( original, forceDuplicate );
        
        IndexedGeometryArray o = (IndexedGeometryArray)original;
        
        this.index = new int[ o.getIndexCount() ];
        this.setIndex( o.getIndex() );
    }
    
    /**
     * 
     * @param type the array type
     * @param coordsSize 23 for 2D, 3 for 3D
     * @param vertexCount the number of vertices (not the nuber of triangles)
     * @param stripVertexCounts the number of vertices per strip
     * @param indexCount the number of indices
     */
    public IndexedGeometryArray( GeometryArrayType type, int coordsSize, int vertexCount, int[] stripVertexCounts, int indexCount )
    {
        super( type, true, coordsSize, vertexCount, stripVertexCounts, indexCount );
        
        this.index = new int[ indexCount ];
    }
}
