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
import org.openmali.spatial.LineContainer;
import org.openmali.vecmath2.Tuple3f;

/**
 * @author David Yazel
 * @author Marvin Froehlich (aka Qudus)
 */
public class IndexedLineArray extends IndexedGeometryArray implements LineContainer
{
    /**
     * {@inheritDoc}
     */
    @Override
    public final boolean isTriangulatable()
    {
        return ( false );
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public final boolean isTriangulated()
    {
        return ( false );
    }
    
    /**
     * {@inheritDoc}
     */
    public final int getLinesCount()
    {
        return ( getIndexCount() / 2 );
    }
    
    /**
     * {@inheritDoc}
     */
    public final boolean getLineCoordinates( int i, Tuple3f start, Tuple3f end )
    {
        if ( i * 2 >= getIndexCount() - 1 )
            return ( false );
        
        getCoordinate( getIndex( i * 2 + 0 ), start );
        getCoordinate( getIndex( i * 2 + 1 ), end );
        
        return ( true );
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void duplicateNodeComponent( NodeComponent original, boolean forceDuplicate )
    {
        super.duplicateNodeComponent( original, forceDuplicate );
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public IndexedLineArray cloneNodeComponent( boolean forceDuplicate )
    {
        IndexedLineArray ila = new IndexedLineArray( this.getCoordinatesSize(), this.getVertexCount(), this.getIndexCount() );
        
        ila.duplicateNodeComponent( this, forceDuplicate );
        
        return ( ila );
    }
    
    /**
     * Constructs an empty IndexedTriangleArray object with the specified
     * number of vertices, vertex format, and number of indices.
     * 
     * @param coordsSize 23 for 2D, 3 for 3D
     * @param vertexCount the number of vertices (not the nuber of triangles)
     * @param indexCount the number of indices
     */
    public IndexedLineArray( int coordsSize, int vertexCount, int indexCount )
    {
        super( GeometryArrayType.LINES, coordsSize, vertexCount, null, indexCount );
    }
    
    /**
     * Constructs an empty IndexedTriangleArray object with the specified
     * number of vertices, vertex format, and number of indices.
     
     * @param vertexCount the number of vertices (not the nuber of triangles)
     * @param indexCount the number of indices
     */
    public IndexedLineArray( int vertexCount, int indexCount )
    {
        this( 3, vertexCount, indexCount );
    }
}
