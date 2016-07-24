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
package org.xith3d.effects.shadows.occluder;

import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;

import org.openmali.vecmath2.Point3f;

import org.xith3d.scenegraph.Geometry;
import org.xith3d.scenegraph.Transform3D;
import org.xith3d.scenegraph.TriangleArray;
import org.xith3d.scenegraph.utils.GeomDrawUtil;
import org.xith3d.utility.comparator.PointComparator;
import org.xith3d.utility.logging.X3DLog;

/**
 * An occluder is a special construction of the geometry of a piece in the scene
 * graph. It consists of a set of faces with connectivity information and a
 * spatial container which partitions the faces such that they can be quickly
 * used in collision checks. An occluder is a primary object for shadow volumes
 * as we can easily calculate the proper edges needed for silhouettes.<br>
 * <br>
 * An occluder is attached to a node in the scenegraph and from that point
 * forward represents the geometry lying below that portion of the scene. It
 * does duplicate some aspects of the GeometryArrays in the shapes below the
 * occluder, but the occluder can be polygon reduced to have a less dense set of
 * triangles.<br>
 * <br>
 * it is important to note that occluders should be fully contiguous shapes
 * such that there are no orphan edges.<br>
 * <br>
 * One thing that can cause issues is the fact that the scenegraph which is
 * represented by a single occluder could actually contain nested transforms
 * which within the local coordinate space of the model. These nested transforms
 * are flattened when building the occluder so that the proper vertices can
 * align.<br>
 * <br>
 * The next problem is that there could be a transform above the model which
 * moves in real time. This would of course normally effect the faces within
 * their local space, as well as their plane calculations. In order to bypass
 * this we will be transforming the location of the lightsource into the local
 * coordinate space so that we can determine the edges. Then when we render the
 * shadow we will use the normal local to world transform for the shadow. (I
 * thought of this in a dream btw... ran downstairs to write it down - Yazel).<br>
 *
 * @author David Yazel
 */
public class Occluder
{
    private class Coord implements Comparable< Coord >
    {
        int index;
        Point3f point;
        
        public Coord( Point3f point )
        {
            this.point = point;
        }
        
        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode()
        {
            // for our special case the hashCode must not "contain" the index.
            return ( point.hashCode() );
        }
        
        public boolean equals( Coord o )
        {
            Point3f p = o.point;
            return ( PointComparator.comparePoints( point, p, 0.001f ) == 0 );
        }
        
        @Override
        public boolean equals( Object o )
        {
            if ( !( o instanceof Coord ) )
                return ( false );
            
            return ( equals( (Coord)o ) );
        }
        
        /**
         * {@inheritDoc}
         */
        public int compareTo( Coord o )
        {
            return ( PointComparator.comparePoints( point, o.point, 0.001f ) );
        }
    }
    
    public Transform3D worldTransform;
    public int nVertices;
    public int nFaces;
    
    // for caching
    private Point3f lastLightPosition = new Point3f();
    private Transform3D lastTransform = new Transform3D();
    
    /* while it makes logical sense to store the occluder information in
     * objects, this makes for very slow loading from disk. We will store the
     * information in multiple arrays, one per element of the OccluderFace.
     */

    // Index Of Each Vertex Within An Object That Makes Up The Triangle Of This
    // Face
    public int[] vertexIndices;
    // 4 elements per face Index Of Each Face That Neighbours This One Within
    // The Object, 3 elements per face
    public float[] planeEquation;
    public int[] neighbourIndices;
    // one element per face
    public boolean[] visible;
    // actual coordinate data, unique points only
    public float[] vertices;
    
    public Geometry buffer = null;
    
    /**
     * Creates a new Occluder.
     */
    public Occluder()
    {
    }
    
    public Geometry getBuffer()
    {
        return ( buffer );
    }
    
    public void determineVisibleEdges( Point3f lightPosition )
    {
        if ( ( worldTransform.compareTo( lastTransform ) != 0 ) || ( !lastLightPosition.equals( lightPosition ) ) )
        {
            Point3f lp = new Point3f( lightPosition );
            Transform3D t = new Transform3D();
            t.set( worldTransform );
            t.invert();
            t.transform( lp );
            
            // Determine Which Faces Are Visible By The Light.
            int numVisible = 0;
            for ( int i = 0; i < nFaces; i++ )
            {
                float side = planeEquation[ i * 4 + 0 ] * lp.getX() + planeEquation[ i * 4 + 1 ] * lp.getY() + planeEquation[ i * 4 + 2 ] * lp.getZ() + planeEquation[ i * 4 + 3 ];
                
                if ( side > 0 )
                {
                    visible[ i ] = true;
                }
                else
                {
                    visible[ i ] = false;
                    numVisible++;
                }
            }
            
            // build the buffer for rendering
            if ( buffer == null )
                buffer = new TriangleArray( nFaces * 3 * 6 );
            
            GeomDrawUtil drawer = new GeomDrawUtil( buffer );
            
            final float INFINITY = 10000;
            int numFragments = 0;
            drawer.drawStart( GeomDrawUtil.CHANGE_COORDINATES );
            
            for ( int i = 0; i < nFaces; i++ )
            {
                if ( visible[ i ] )
                {
                    // Go Through Each Edge
                    for ( int j = 0; j < 3; j++ )
                    {
                        int neighbourIndex = neighbourIndices[ i * 3 + j ];
                        
                        // If There Is No Neighbour, Or Its Neighbouring Face Is
                        // Not Visible, Then This Edge Casts A Shadow
                        if ( neighbourIndex == -1 || ( visible[ neighbourIndex ] == false ) )
                        {
                            numFragments++;
                            // Get The Points On The Edge
                            
                            float v1x = vertices[ vertexIndices[ i * 3 + j ] * 3 + 0 ];
                            float v1y = vertices[ vertexIndices[ i * 3 + j ] * 3 + 1 ];
                            float v1z = vertices[ vertexIndices[ i * 3 + j ] * 3 + 2 ];
                            
                            int k = ( j + 1 ) % 3;
                            
                            float v2x = vertices[ vertexIndices[ i * 3 + k ] * 3 + 0 ];
                            float v2y = vertices[ vertexIndices[ i * 3 + k ] * 3 + 1 ];
                            float v2z = vertices[ vertexIndices[ i * 3 + k ] * 3 + 2 ];
                            
                            // Calculate The Two Vertices In Distance
                            float v3x = ( v1x - lp.getX() ) * INFINITY;
                            float v3y = ( v1y - lp.getY() ) * INFINITY;
                            float v3z = ( v1z - lp.getZ() ) * INFINITY;
                            
                            float v4x = ( v2x - lp.getX() ) * INFINITY;
                            float v4y = ( v2y - lp.getY() ) * INFINITY;
                            float v4z = ( v2z - lp.getZ() ) * INFINITY;
                            
                            // Draw The Quadrilateral (As A Triangle Strip)
                            drawer.setCoordinate( v1x, v1y, v1z );
                            drawer.setCoordinate( v1x + v3x, v1y + v3y, v1z + v3z );
                            drawer.setCoordinate( v2x + v4x, v2y + v4y, v2z + v4z );
                            
                            drawer.setCoordinate( v1x, v1y, v1z );
                            drawer.setCoordinate( v2x + v4x, v2y + v4y, v2z + v4z );
                            drawer.setCoordinate( v2x, v2y, v2z );
                        }
                    }
                }
            }
            
            // TODO : Check
            // buffer.setValidVertexCount(numFragments*6);
            buffer.setValidVertexCount( buffer.getCoordinatesData().getCount() / 3 );
        }
    }
    
    /**
     * Takes a list of occluder submission nodes and builds the vertex and face
     * lists
     * 
     * @param submission A list of OccluderSubmission nodes
     */
    public void build( List< OccluderSubmission > submission )
    {
        // step through and determine the total number of faces and
        // vertices we need to allocate;
        nVertices = 0;
        nFaces = 0;
        for ( OccluderSubmission os: submission )
        {
            nVertices += os.shape.getGeometry().getVertexCount();
        }
        nFaces = nVertices / 3;
        
        // allocate face based arays
        
        vertexIndices = new int[ nFaces * 3 ];
        planeEquation = new float[ nFaces * 4 ];
        neighbourIndices = new int[ nFaces * 3 ];
        visible = new boolean[ nFaces ];
        
        Arrays.fill( neighbourIndices, -1 );
        
        /*
         * now we have the number of faces, but we don't know how many unique
         * vertices we have, so we need to build a unique list of them. This is
         * a bit expensive, but it would be more expensive to keep all those
         * vertices around;
         */

        Point3f p = new Point3f();
        Coord testCoord = new Coord( p );
        TreeMap< Coord, Coord > vertexMap = new TreeMap< Coord, Coord >();
        for ( OccluderSubmission os: submission )
        {
            Geometry ga = os.shape.getGeometry();
            
            // get each point
            final int j0 = ga.getInitialVertexIndex();
            final int shapeVertices = ga.getValidVertexCount();
            for ( int j = j0; j < shapeVertices; j++ )
            {
                ga.setCoordinate( j, p );
                
                Coord newCoord = new Coord( new Point3f( p ) );
                vertexMap.put( testCoord, newCoord );
                
                assert vertexMap.get( testCoord ) != null : "Cannot find the point just entered :" + p;
            }
        }
        
        // ok, now we have a map of all unique vertices. We need to now
        // save them in the data array. We will assign the index values
        // to the tree nodes so we can look them up in the next pass
        
        int vIndex = 0;
        vertices = new float[ vertexMap.size() * 3 ];
        for ( Coord c: vertexMap.values() )
        {
            c.index = vIndex;
            vertices[ vIndex * 3 + 0 ] = c.point.getX();
            vertices[ vIndex * 3 + 1 ] = c.point.getY();
            vertices[ vIndex * 3 + 2 ] = c.point.getZ();
            vIndex++;
        }
        
        // step back through the faces and get the vertices
        
        int curFace = 0;
        
        for ( OccluderSubmission os: submission )
        {
            Geometry ga = os.shape.getGeometry();
            
            // step through all the faces in the shape
            
            final int shapeVertices = ga.getValidVertexCount();
            for ( int j = 0; j < shapeVertices; j++ )
            {
                ga.getCoordinate( j, p );
                
                Coord match = vertexMap.get( testCoord );
                if ( match == null )
                {
                    final String msg = "Cannot find matching coord map for face " + curFace + " corner " + ( j % 3 ) + " which has coord " + p;
                    //System.err.println( msg );
                    throw new Error( msg );
                }
                
                vertexIndices[ j ] = match.index;
                
                if ( j % 3 == 2 )
                {
                    calculateFacePlane( curFace );
                    
                    curFace++;
                }
            }
        }
        
        calculateConnectivity();
    }
    
    private void calculateFacePlane( int face )
    {
        // Get Shortened Names For The Vertices Of The Face
        
        final Point3f v1 = new Point3f();
        final Point3f v2 = new Point3f();
        final Point3f v3 = new Point3f();
        
        v1.set( vertices[ vertexIndices[ face * 3 + 0 ] * 3 + 0 ], vertices[ vertexIndices[ face * 3 + 0 ] * 3 + 1 ], vertices[ vertexIndices[ face * 3 + 0 ] * 3 + 2 ] );
        v2.set( vertices[ vertexIndices[ face * 3 + 1 ] * 3 + 0 ], vertices[ vertexIndices[ face * 3 + 1 ] * 3 + 1 ], vertices[ vertexIndices[ face * 3 + 1 ] * 3 + 2 ] );
        v3.set( vertices[ vertexIndices[ face * 3 + 2 ] * 3 + 0 ], vertices[ vertexIndices[ face * 3 + 2 ] * 3 + 1 ], vertices[ vertexIndices[ face * 3 + 2 ] * 3 + 2 ] );
        
        float a = v1.getY() * ( v2.getZ() - v3.getZ() ) + v2.getY() * ( v3.getZ() - v1.getZ() ) + v3.getY() * ( v1.getZ() - v2.getZ() );
        float b = v1.getZ() * ( v2.getX() - v3.getX() ) + v2.getZ() * ( v3.getX() - v1.getX() ) + v3.getZ() * ( v1.getX() - v2.getX() );
        float c = v1.getX() * ( v2.getY() - v3.getY() ) + v2.getX() * ( v3.getY() - v1.getY() ) + v3.getX() * ( v1.getY() - v2.getY() );
        float d = -( v1.getX() * ( v2.getY() * v3.getZ() - v3.getY() * v2.getZ() ) + v2.getX() * ( v3.getY() * v1.getZ() - v1.getY() * v3.getZ() ) + v3.getX() * ( v1.getY() * v2.getZ() - v2.getY() * v1.getZ() ) );
        
        // save off the plane equation
        
        planeEquation[ face * 4 + 0 ] = a;
        planeEquation[ face * 4 + 1 ] = b;
        planeEquation[ face * 4 + 2 ] = c;
        planeEquation[ face * 4 + 3 ] = d;
    }
    
    private void calculateConnectivity()
    {
        int numConnected = 0;
        
        for ( int faceA = 0; faceA < nFaces; faceA++ )
        {
            for ( int edgeA = 0; edgeA < 3; edgeA++ )
            {
                if ( neighbourIndices[ faceA * 3 + edgeA ] == -1 )
                {
                    boolean edgeFound = false;
                    for ( int faceB = 0; faceB < nFaces; faceB++ )
                    {
                        if ( faceA != faceB )
                        {
                            for ( int edgeB = 0; edgeB < 3; edgeB++ )
                            {
                                int vertA1 = vertexIndices[ faceA * 3 + edgeA ];
                                int vertA2 = vertexIndices[ faceA * 3 + ( edgeA + 1 ) % 3 ];
                                
                                int vertB1 = vertexIndices[ faceB * 3 + edgeB ];
                                int vertB2 = vertexIndices[ faceB * 3 + ( edgeB + 1 ) % 3 ];
                                
                                // Check If They Are Neighbours - IE, The Edges
                                // Are The Same
                                if ( ( vertA1 == vertB1 && vertA2 == vertB2 ) || ( vertA1 == vertB2 && vertA2 == vertB1 ) )
                                {
                                    neighbourIndices[ faceA * 3 + edgeA ] = faceB;
                                    neighbourIndices[ faceB * 3 + edgeB ] = faceA;
                                    
                                    edgeFound = true;
                                    numConnected++;
                                    break;
                                }
                            }
                            
                            if ( edgeFound )
                                break;
                        }
                    }
                }
            }
        }
        
        X3DLog.debug( "connected ", numConnected, " edges out of ", nFaces * 3 );
    }
    
    public void setWorldTransform( Transform3D worldTransform )
    {
        this.worldTransform = worldTransform;
    }
    
    public Transform3D getWorldTransform()
    {
        return ( worldTransform );
    }
    
    public int[] getNeighbourIndices()
    {
        return ( neighbourIndices );
    }
    
    public int getNFaces()
    {
        return ( nFaces );
    }
    
    public int getNVertices()
    {
        return ( nVertices );
    }
    
    public float[] getPlaneEquation()
    {
        return ( planeEquation );
    }
    
    public int[] getVertexIndices()
    {
        return ( vertexIndices );
    }
    
    public float[] getVertices()
    {
        return ( vertices );
    }
    
    public boolean[] getVisible()
    {
        return ( visible );
    }
}
