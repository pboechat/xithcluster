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
package org.xith3d.render.lwjgl;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;

import org.jagatoo.logging.ProfileTimer;
import org.jagatoo.opengl.enums.CompareFunction;
import org.jagatoo.opengl.enums.TextureCompareMode;
import org.jagatoo.opengl.enums.TextureFilter;
import org.jagatoo.opengl.enums.TextureFormat;
import org.jagatoo.opengl.enums.TextureImageInternalFormat;
import org.jagatoo.opengl.enums.TextureMode;
import org.jagatoo.opengl.enums.TextureType;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.ARBShadow;
import org.lwjgl.opengl.ARBTextureEnvCombine;
import org.lwjgl.opengl.EXTTextureEnvCombine;
import org.lwjgl.opengl.EXTTextureFilterAnisotropic;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.openmali.types.twodee.Rect2i;
import org.openmali.vecmath2.Colorf;
import org.xith3d.render.CanvasPeer;
import org.xith3d.render.OpenGLCapabilities;
import org.xith3d.render.OpenGLInfo;
import org.xith3d.render.OpenGLStatesCache;
import org.xith3d.render.OpenGlExtensions;
import org.xith3d.render.RenderOptions;
import org.xith3d.render.RenderPeer;
import org.xith3d.render.SceneGraphOpenGLReference;
import org.xith3d.render.SceneGraphOpenGLReferences;
import org.xith3d.render.RenderPeer.RenderMode;
import org.xith3d.render.preprocessing.RenderAtom;
import org.xith3d.render.states.StateUnit;
import org.xith3d.render.states.units.StateUnitPeer;
import org.xith3d.render.states.units.TextureUnitStateUnit;
import org.xith3d.scenegraph.Appearance;
import org.xith3d.scenegraph.GlobalOptions;
import org.xith3d.scenegraph.ProjectiveTextureUnit;
import org.xith3d.scenegraph.Shape3D;
import org.xith3d.scenegraph.TexCoordGeneration;
import org.xith3d.scenegraph.Texture;
import org.xith3d.scenegraph.Texture2D;
import org.xith3d.scenegraph.Texture3D;
import org.xith3d.scenegraph.TextureAttributes;
import org.xith3d.scenegraph.TextureCubeMap;
import org.xith3d.scenegraph.TextureImage;
import org.xith3d.scenegraph.TextureImage2D;
import org.xith3d.scenegraph.TextureImage3D;
import org.xith3d.scenegraph.TextureUnit;
import org.xith3d.scenegraph.Transform3D;
import org.xith3d.scenegraph.View;
import org.xith3d.scenegraph._SG_PrivilegedAccess;
import org.xith3d.utility.logging.X3DLog;

/**
 * @author David Yazel
 * @author Marvin Froehlich (aka Qudus)
 */
public class TextureUnitStateUnitPeer implements StateUnitPeer
{
    private static final IntBuffer tmpIntBuffer = BufferUtils.createIntBuffer( 1 );
    
    private static SceneGraphOpenGLReferences.Provider textureNameProvider = new SceneGraphOpenGLReferences.Provider()
    {
        public SceneGraphOpenGLReference newReference( CanvasPeer canvasPeer, SceneGraphOpenGLReferences references, int numNamesPerContext )
        {
            return ( new SceneGraphOpenGLReference( canvasPeer, references, numNamesPerContext )
            {
                @Override
                public void prepareObjectForDestroy()
                {
                    SceneGraphOpenGLReference ref = getReferences().removeReference( getContext().getCanvasID() );
                    
                    ( (CanvasPeerImplBase)getContext() ).addDestroyableObject( ref );
                }
                
                @Override
                public void destroyObject( int index, int name )
                {
                    tmpIntBuffer.clear();
                    tmpIntBuffer.put( name ).flip();
                    GL11.glDeleteTextures( tmpIntBuffer );
                }
            } );
        }
    };
    
    private static final FloatBuffer tmpBorderColor = BufferUtils.createFloatBuffer( 4 );
    private static final FloatBuffer texBlendColor = BufferUtils.createFloatBuffer( 4 );
    private static final FloatBuffer tmpPlaneBuffer = BufferUtils.createFloatBuffer( 4 );
    private static final ByteBuffer textureDataBuffer = BufferUtils.createByteBuffer( 512 * 512 * 4 );
    
    private static final FloatBuffer DEFAULT_TEXTURE_BLEND_COLOR = BufferUtils.createFloatBuffer( 4 );
    static
    {
        DEFAULT_TEXTURE_BLEND_COLOR.put( 0, 0f );
        DEFAULT_TEXTURE_BLEND_COLOR.put( 1, 0f );
        DEFAULT_TEXTURE_BLEND_COLOR.put( 2, 0f );
        DEFAULT_TEXTURE_BLEND_COLOR.put( 3, 1f );
        DEFAULT_TEXTURE_BLEND_COLOR.rewind();
    }
    
    
    public static final int getMaxAnisotropicLevel()
    {
        if ( !OpenGlExtensions.GL_EXT_texture_filter_anisotropic )
            return ( 0 );
        
        FloatBuffer buffer = BufferUtils.createFloatBuffer( 16 );
        buffer.rewind();
        
        GL11.glGetFloat( EXTTextureFilterAnisotropic.GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT, buffer );
        
        return ( (int)buffer.get( 0 ) );
    }
    
    public static final void selectServerTextureUnit( int unit, OpenGLStatesCache statesCache, boolean force )
    {
        if ( ( statesCache.enabled && statesCache.currentServerTextureUnit == unit ) && !force )
            return;
        
        X3DLog.debug( "Activating (server) texture unit ", unit );
        
        final int glUnit = GL13.GL_TEXTURE0 + unit;
        
        GL13.glActiveTexture( glUnit );
        
        statesCache.currentServerTextureUnit = unit;
    }
    
    public static final int translateInternalFormat( TextureFormat format, TextureImageInternalFormat internalFormat, int depthBuffersize )
    {
        if ( format == TextureFormat.DEPTH )
        {
            if ( depthBuffersize == 16 )
                internalFormat = TextureImageInternalFormat.DEPTH16;
            else if ( depthBuffersize == 24 )
                internalFormat = TextureImageInternalFormat.DEPTH24;
            else if ( depthBuffersize == 32 )
                internalFormat = TextureImageInternalFormat.DEPTH32;
        }
        
        return ( internalFormat.toOpenGL() );
    }
    
    private static final int getCubeMapFace( int i )
    {
        /*
        switch ( i )
        {
            case 0:
                return ( GL13.GL_TEXTURE_CUBE_MAP_POSITIVE_X );
            case 1:
                return ( GL13.GL_TEXTURE_CUBE_MAP_NEGATIVE_X );
            case 2:
                return ( GL13.GL_TEXTURE_CUBE_MAP_POSITIVE_Y );
            case 3:
                return ( GL13.GL_TEXTURE_CUBE_MAP_NEGATIVE_Y );
            case 4:
                return ( GL13.GL_TEXTURE_CUBE_MAP_POSITIVE_Z );
            case 5:
                return ( GL13.GL_TEXTURE_CUBE_MAP_NEGATIVE_Z );
            default:
                Error e = new Error( "Unrecognized cube map face: " + i );
                X3DLog.print( e );
                throw e );
        }
        */
        
        return ( GL13.GL_TEXTURE_CUBE_MAP_POSITIVE_X + i );
    }
    
    private static final void applyTextureAttachedAttributes( Texture texture, boolean mipmapping, OpenGLCapabilities glCaps )
    {
        final TextureType texType = texture.getType();
        //if ( ( type == Texture.Type.TEXTURE_2D ) || ( type == Texture.Type.TEXTURE_1D ) )
        final int glTexType = texType.toOpenGL();
        
        /*
         * Setup texture boundary...
         */
        
        GL11.glTexParameteri( glTexType, GL11.GL_TEXTURE_WRAP_S, texture.getBoundaryModeS().toOpenGL() );
        GL11.glTexParameteri( glTexType, GL11.GL_TEXTURE_WRAP_T, texture.getBoundaryModeT().toOpenGL() );
        
        if ( texType == TextureType.TEXTURE_3D )
        {
            GL11.glTexParameteri( glTexType, GL12.GL_TEXTURE_WRAP_R, ( (Texture3D)texture ).getBoundaryModeR().toOpenGL() );
        }
        
        // boundary color
        texture.getBoundaryColor().writeToBuffer( true, tmpBorderColor, true, true );
        GL11.glTexParameter( glTexType, GL11.GL_TEXTURE_BORDER_COLOR, tmpBorderColor );
        
        /*
         * Setup texture filters...
         */
        
        TextureFilter filter = texture.getFilter();
        
        if ( filter == null )
        {
            filter = GlobalOptions.getInstance().getTextureFilter();
        }
        
        GL11.glTexParameteri( glTexType, GL11.GL_TEXTURE_MAG_FILTER, filter.getOpenGLMagFilter() );
        GL11.glTexParameteri( glTexType, GL11.GL_TEXTURE_MIN_FILTER, filter.getOpenGLMinFilter( mipmapping ) );
        
        if ( OpenGlExtensions.GL_EXT_texture_filter_anisotropic )
        {
            int aniso = filter.getAnisotropicLevel();
            int maxAniso = glCaps.getMaxAnisotropicLevel();
            
            if ( aniso > maxAniso )
                aniso = maxAniso;
            
            GL11.glTexParameteri( glTexType, EXTTextureFilterAnisotropic.GL_TEXTURE_MAX_ANISOTROPY_EXT, aniso );
        }
    }
    
    private static final int defineTexture( int unit, Texture texture, CanvasPeer canvasPeer, int depthBufferSize, OpenGLStatesCache statesCache )
    {
        final int numMipmaps = texture.getImagesCount();
        
        if ( numMipmaps == 0 )
        {
            X3DLog.debug( "Found texture without images. Skipping!" );
            return ( -1 );
        }
        
        /*
        if ( texture instanceof Texture2D )
        {
            if ( ( (Texture2D)texture ).hasTextureCanvas() )
            {
                _SG_PrivilegedAccess.notifyDrawCallbacks( ( (Texture2D)texture ).getTextureCanvas(), nanoTime );
            }
        }
        */
        
        SceneGraphOpenGLReference openGLRef = texture.getOpenGLReferences().getReference( canvasPeer, textureNameProvider );
        
        int textureId = openGLRef.getName();
        if ( textureId == -1 )
        {
            tmpIntBuffer.clear();
            GL11.glGenTextures( tmpIntBuffer );
            textureId = tmpIntBuffer.get( 0 );
            openGLRef.setName( textureId );
        }
        
        //X3DLog.debug( "Binding texture to handle ", textureId );
        
        final int glTexType = texture.getType().toOpenGL();
        
        GL11.glBindTexture( glTexType, textureId );
        statesCache.currentBoundTexture[ unit ] = texture;
        
        switch ( glTexType )
        {
            case GL11.GL_TEXTURE_2D:
            {
                TextureImage2D image = null;
                int format = 0;
                int internalFormat = 0;
                final int border = texture.getBoundaryWidth();
                
                for ( int level = 0; level < numMipmaps; level++ )
                {
                    image = (TextureImage2D)texture.getImage( level );
                    
                    if ( image.hasData() )
                    {
                        format = image.getFormat().toOpenGL();
                        internalFormat = translateInternalFormat( texture.getFormat(), image.getInternalFormat(), depthBufferSize );
                        
                        ByteBuffer buff;
                        if ( image.getDataBuffer() != null )
                        {
                            buff = image.getDataBuffer();
                        }
                        else if ( image.getDataSize() > textureDataBuffer.capacity() )
                        {
                            buff = BufferUtils.createByteBuffer( image.getDataSize() );
                            image.getData( buff );
                        }
                        else
                        {
                            buff = textureDataBuffer;
                            image.getData( buff );
                        }
                        
                        if ( image.isCompressed() )
                        {
                            GL13.glCompressedTexImage2D( glTexType, level, internalFormat, image.getWidth(), image.getHeight(), border, buff );
                        }
                        else
                        {
                            GL11.glTexImage2D( glTexType, level, internalFormat, image.getWidth(), image.getHeight(), border, format, GL11.GL_UNSIGNED_BYTE, buff );
                        }
                        
                        if ( texture.isMarkedAsLocalDataToBeFreed() )
                        {
                            image.freeLocalData();
                            RenderPeerImpl.setGCRequested();
                        }
                    }
                }
            }
            break;
            case GL12.GL_TEXTURE_3D:
            {
                TextureImage3D image = null;
                int format = 0;
                int internalFormat = 0;
                int border = texture.getBoundaryWidth();
                int depth = 0;
                
                for ( int level = 0; level < numMipmaps; level++ )
                {
                    image = (TextureImage3D)texture.getImage( level );
                    internalFormat = translateInternalFormat( texture.getFormat(), image.getInternalFormat(), depthBufferSize );
                    depth = image.getDepth();
                    
                    ByteBuffer buff = image.getDataBuffer();
                    
                    if ( image.isCompressed() )
                    {
                        GL13.glCompressedTexImage3D( glTexType, level, internalFormat, image.getWidth() + 2 * border, image.getHeight() + 2 * border, depth, border, buff );
                    }
                    else
                    {
                        format = image.getFormat().toOpenGL();
                        GL12.glTexImage3D( glTexType, level, internalFormat, image.getWidth() + 2 * border, image.getHeight() + 2 * border, depth, border, format, GL11.GL_UNSIGNED_BYTE, buff );
                    }
                }
            }
            break;
            case GL13.GL_TEXTURE_CUBE_MAP:
            {
                // if ((texType == GL.GL_TEXTURE_CUBE_MAP) &&
                // (OpenGlExtensions.GL_ARB_texture_cube_map)) {
                for ( int i = 0; i < 6; i++ )
                {
                    TextureImage image;
                    int face = 0;
                    int format;
                    int internalFormat;
                    final int border = 0; // !!! what does this do? !!!
                    
                    for ( int level = 0; level < numMipmaps; level++ )
                    {
                        image = ( (TextureCubeMap)texture ).getImage( level, i );
                        format = image.getFormat().toOpenGL();
                        internalFormat = translateInternalFormat( texture.getFormat(), image.getInternalFormat(), depthBufferSize );
                        face = getCubeMapFace( i );
                        
                        ByteBuffer buff;
                        if ( image instanceof TextureImage2D )
                        {
                            final TextureImage2D image2D = (TextureImage2D)image;
                            
                            if ( image2D.getDataBuffer() != null )
                            {
                                buff = image2D.getDataBuffer();
                            }
                            else if ( image2D.getDataSize() > textureDataBuffer.capacity() )
                            {
                                buff = BufferUtils.createByteBuffer( image2D.getDataSize() );
                                image2D.getData( buff );
                            }
                            else
                            {
                                buff = textureDataBuffer;
                                image2D.getData( buff );
                            }
                        }
                        else if ( image instanceof TextureImage3D )
                        {
                            final TextureImage3D image3D = (TextureImage3D)image;
                            
                            buff = image3D.getDataBuffer();
                        }
                        else
                        {
                            throw new Error( "Unknown TextureImage type " + image.getClass() );
                        }
                        
                        if ( image.isCompressed() )
                        {
                            GL13.glCompressedTexImage2D( face, level, internalFormat, image.getWidth(), image.getHeight(), border, buff );
                        }
                        else
                        {
                            GL11.glTexImage2D( face, level, internalFormat, image.getWidth(), image.getHeight(), border, format, GL11.GL_UNSIGNED_BYTE, buff );
                        }
                    }
                }
            }
            break;
        }
        
        return ( textureId );
    }
    
    private static final void updateTexture( Texture2D texture )
    {
        /*
        if ( texture.hasTextureCanvas() )
        {
            _SG_PrivilegedAccess.notifyDrawCallbacks( texture.getTextureCanvas() );
        }
        */
        
        if ( !texture.hasUpdateList() )
            return;
        
        final int numLevel = texture.getImagesCount();
        for ( int level = 0; level < numLevel; level++ )
        {
            final TextureImage2D image = (TextureImage2D)texture.getImage( level );
            final ArrayList< Rect2i > list = image.getUpdateList();
            if ( !list.isEmpty() )
            {
                final int format = image.getFormat().toOpenGL();
                
                final int listSize = list.size();
                for ( int i = 0; i < listSize; i++ )
                {
                    Rect2i r = list.get( i );
                    
                    GL11.glPixelStorei( GL11.GL_UNPACK_ROW_LENGTH, image.getWidth() );
                    GL11.glPixelStorei( GL11.GL_UNPACK_SKIP_PIXELS, r.getLeft() );
                    GL11.glPixelStorei( GL11.GL_UNPACK_SKIP_ROWS, r.getTop() );
                    
                    ByteBuffer buff;
                    if ( image.getDataBuffer() != null )
                    {
                        buff = image.getDataBuffer();
                    }
                    else if ( image.getDataSize() > textureDataBuffer.capacity() )
                    {
                        buff = BufferUtils.createByteBuffer( image.getDataSize() );
                        image.getData( buff );
                    }
                    else
                    {
                        buff = textureDataBuffer;
                        image.getData( buff );
                    }
                    
                    // define the sub image
                    GL11.glTexSubImage2D( GL11.GL_TEXTURE_2D, level, r.getLeft(), r.getTop(), r.getWidth(), r.getHeight(), format, GL11.GL_UNSIGNED_BYTE, buff );
                }
                image.clearUpdateList();
                
                GL11.glPixelStorei( GL11.GL_UNPACK_ROW_LENGTH, 0 );
                GL11.glPixelStorei( GL11.GL_UNPACK_SKIP_PIXELS, 0 );
                GL11.glPixelStorei( GL11.GL_UNPACK_SKIP_ROWS, 0 );
            }
        }
        
        texture.setHasUpdateList( false );
    }
    
    private static final int bindTexture( OpenGLStatesCache statesCache, Texture texture, int unit, CanvasPeer canvasPeer, int depthBuffersize )
    {
        final TextureType texType = texture.getType();
        
        final boolean tex2DEnabled;
        final boolean tex3DEnabled;
        final boolean texCMEnabled;
        
        switch ( texType )
        {
            case TEXTURE_CUBE_MAP:
                tex2DEnabled = false;
                tex3DEnabled = false;
                texCMEnabled = true;
                break;
            case TEXTURE_3D:
                tex2DEnabled = false;
                tex3DEnabled = true;
                texCMEnabled = false;
                break;
            case TEXTURE_2D:
            default:
                tex2DEnabled = true;
                tex3DEnabled = false;
                texCMEnabled = false;
                break;
        }
        
        if ( tex2DEnabled && ( !statesCache.enabled || !statesCache.texture2DEnabled[ unit ] ) )
            GL11.glEnable( GL11.GL_TEXTURE_2D );
        else if ( !tex2DEnabled && ( !statesCache.enabled || statesCache.texture2DEnabled[ unit ] ) )
            GL11.glDisable( GL11.GL_TEXTURE_2D );
        if ( tex3DEnabled && ( !statesCache.enabled || !statesCache.texture3DEnabled[ unit ] ) )
            GL11.glEnable( GL12.GL_TEXTURE_3D );
        else if ( !tex3DEnabled && ( !statesCache.enabled || statesCache.texture3DEnabled[ unit ] ) )
            GL11.glDisable( GL12.GL_TEXTURE_3D );
        if ( texCMEnabled && ( !statesCache.enabled || !statesCache.textureCMEnabled[ unit ] ) )
            GL11.glEnable( GL13.GL_TEXTURE_CUBE_MAP );
        else if ( !texCMEnabled && ( !statesCache.enabled || statesCache.textureCMEnabled[ unit ] ) )
            GL11.glDisable( GL13.GL_TEXTURE_CUBE_MAP );
        
        statesCache.texture2DEnabled[ unit ] = tex2DEnabled;
        statesCache.texture3DEnabled[ unit ] = tex3DEnabled;
        statesCache.textureCMEnabled[ unit ] = texCMEnabled;
        
        SceneGraphOpenGLReference openGLRef = texture.getOpenGLReferences().getReference( canvasPeer, textureNameProvider );
        int texHandle = openGLRef.getName();
        
        if ( texture.isDirty() )
        {
            if ( texHandle != -1 )
            {
                tmpIntBuffer.clear();
                tmpIntBuffer.put( texHandle ).flip();
                GL11.glDeleteTextures( tmpIntBuffer );
                texHandle = openGLRef.deleteName();
            }
            
            _SG_PrivilegedAccess.setDirty( texture, false );
        }
        
        if ( ( texHandle != -1 ) && ( !texture.hasSizeChanged() ) )
        {
            //X3DLog.debug( "Already cached, so binding texture" );
            GL11.glBindTexture( texType.toOpenGL(), texHandle ); // TODO: cache in OpenGLStatesCache?
            statesCache.currentBoundTexture[ unit ] = texture;
            
            if ( texType == TextureType.TEXTURE_2D )
            {
                updateTexture( (Texture2D)texture );
            }
        }
        else
        {
            texHandle = defineTexture( unit, texture, canvasPeer, depthBuffersize, statesCache );
            
            _SG_PrivilegedAccess.resetSizeChanged( texture );
        }
        
        return ( texHandle );
    }
    
    private static final int setTextureState( OpenGLCapabilities glCaps, OpenGLStatesCache statesCache, Texture texture, int unit, boolean texChanged, CanvasPeer canvasPeer, int depthBuffersize )
    {
        final int texHandle = bindTexture( statesCache, texture, unit, canvasPeer, depthBuffersize );
        
        if ( texChanged )
        {
            applyTextureAttachedAttributes( texture, ( texture.getImagesCount() > 1 ), glCaps );
            
            _SG_PrivilegedAccess.setChanged( texture, false );
        }
        
        return ( texHandle );
    }
    
    private static final void disableTextureState( OpenGLStatesCache statesCache, Texture texture, int unit, boolean texChanged )
    {
        selectServerTextureUnit( unit, statesCache, false );
        
        if ( !statesCache.enabled || statesCache.texture2DEnabled[ unit ] )
        {
            GL11.glDisable( GL11.GL_TEXTURE_2D );
            statesCache.texture2DEnabled[ unit ] = false;
        }
        if ( !statesCache.enabled || statesCache.texture3DEnabled[ unit ] )
        {
            GL11.glDisable( GL12.GL_TEXTURE_3D );
            statesCache.texture3DEnabled[ unit ] = false;
        }
        if ( !statesCache.enabled || statesCache.textureCMEnabled[ unit ] )
        {
            GL11.glDisable( GL13.GL_TEXTURE_CUBE_MAP );
            statesCache.textureCMEnabled[ unit ] = false;
        }
        
        if ( texChanged )
        {
            _SG_PrivilegedAccess.setChanged( texture, false );
        }
    }
    
    protected static final int setTextureState2( OpenGLCapabilities glCaps, OpenGLStatesCache statesCache, Texture texture, int unit, boolean texChanged, CanvasPeer canvasPeer, int depthBuffersize )
    {
        if ( ( texture == null ) || !texture.isEnabled() )
        {
            disableTextureState( statesCache, texture, unit, texChanged );
            
            return ( -1 );
        }
        
        return ( setTextureState( glCaps, statesCache, texture, unit, texChanged, canvasPeer, depthBuffersize ) );
    }
    
    private static final void setCompareMode( OpenGLInfo glInfo, int glTexType, TextureCompareMode mode )
    {
        if ( ( glInfo.getVersionMajor() > 1 ) || ( glInfo.getVersionMinor() >= 4 ) )
        {
            GL11.glTexParameteri( glTexType, GL14.GL_TEXTURE_COMPARE_MODE, mode.toOpenGL() );
        }
        else if ( OpenGlExtensions.ARB_shadow )
        {
            GL11.glTexParameteri( glTexType, ARBShadow.GL_TEXTURE_COMPARE_MODE_ARB, ARBShadow.GL_COMPARE_R_TO_TEXTURE_ARB );
        }
    }
    
    private static final void setCompareFunc( OpenGLInfo glInfo, int glTexType, CompareFunction func )
    {
        if ( glInfo.getNormalizedVersion() >= OpenGLInfo.NORM_VERSION_1_4 )
        {
            GL11.glTexParameteri( glTexType, GL14.GL_TEXTURE_COMPARE_FUNC, func.toOpenGL() );
        }
        else if ( OpenGlExtensions.ARB_shadow )
        {
            GL11.glTexParameteri( glTexType, ARBShadow.GL_TEXTURE_COMPARE_FUNC_ARB, func.toOpenGL() );
        }
    }
    
    private static final void setTextureMatrix( Transform3D trans )
    {
        GL11.glMatrixMode( GL11.GL_TEXTURE );
        GL11.glLoadMatrix( _SG_PrivilegedAccess.getFloatBuffer( trans, true ) );
        GL11.glMatrixMode( GL11.GL_MODELVIEW );
    }
    
    private static final void setIdentityTexMat()
    {
        GL11.glMatrixMode( GL11.GL_TEXTURE );
        GL11.glLoadIdentity();
        GL11.glMatrixMode( GL11.GL_MODELVIEW );
    }
    
    private void setTextureAttributes( int unit, TextureType texType, TextureAttributes ta, OpenGLInfo glInfo, OpenGLStatesCache statesCache )
    {
        if ( ( ta == null ) || ( ta.getTextureTransform() == null ) )
        {
            if ( !statesCache.enabled || statesCache.currentTextureMatrix[ unit ] != null )
            {
                setIdentityTexMat();
                statesCache.currentTextureMatrix[ unit ] = null;
            }
        }
        
        if ( ta == null )
        {
            if ( !statesCache.enabled || statesCache.currentTextureMode[ unit ] != TextureMode.MODULATE )
            {
                GL11.glTexEnvi( GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, GL11.GL_MODULATE );
                statesCache.currentTextureMode[ unit ] = TextureMode.MODULATE;
            }
            
            return;
        }
        
        final Transform3D texTrans = ta.getTextureTransform();
        if ( texTrans != null )
        {
            if ( ( !statesCache.enabled || statesCache.currentTextureMatrix[ unit ] != texTrans ) || texTrans.isChanged() )
            {
                setTextureMatrix( texTrans );
                
                statesCache.currentTextureMatrix[ unit ] = texTrans;
                _SG_PrivilegedAccess.setChanged( texTrans, false );
            }
        }
        
        final TextureMode textureMode = ta.getTextureMode();
        
        if ( !statesCache.enabled || statesCache.currentTextureMode[ unit ] != textureMode )
        {
            GL11.glTexEnvi( GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, textureMode.toOpenGL() );
            
            statesCache.currentTextureMode[ unit ] = textureMode;
        }
        
        if ( textureMode == TextureMode.COMBINE )
        {
            Colorf tbc = ta.getTextureBlendColor();
            if ( tbc == null )
            {
                if ( !statesCache.enabled || statesCache.currentTextureBlendColor[ unit ] != null )
                {
                    GL11.glTexEnv( GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_COLOR, DEFAULT_TEXTURE_BLEND_COLOR );
                    statesCache.currentTextureBlendColor[ unit ] = null;
                }
            }
            else
            {
                if ( ( !statesCache.enabled || statesCache.currentTextureBlendColor[ unit ] != tbc ) || tbc.isDirty() )
                {
                    /*
                    texBlendColor.put( 0, tbc.getRed() );
                    texBlendColor.put( 1, tbc.getGreen() );
                    texBlendColor.put( 2, tbc.getBlue() );
                    texBlendColor.put( 3, tbc.getAlpha() );
                    */
                    tbc.writeToBuffer( true, texBlendColor, true, true );
                    GL11.glTexEnv( GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_COLOR, texBlendColor );
                    statesCache.currentTextureBlendColor[ unit ] = tbc;
                    tbc.setClean();
                }
            }
            
            
            // setup rgb
            
            if ( !statesCache.enabled || statesCache.currentCombineMode_RGB[ unit ] != ta.getCombineRGBMode() )
            {
                GL11.glTexEnvf( GL11.GL_TEXTURE_ENV, EXTTextureEnvCombine.GL_COMBINE_RGB_EXT, ta.getCombineRGBMode().toOpenGL() );
                statesCache.currentCombineMode_RGB[ unit ] = ta.getCombineRGBMode();
            }
            if ( !statesCache.enabled || statesCache.currentCombineMode_Alpha[ unit ] != ta.getCombineAlphaMode() )
            {
                GL11.glTexEnvf( GL11.GL_TEXTURE_ENV, EXTTextureEnvCombine.GL_COMBINE_ALPHA_EXT, ta.getCombineAlphaMode().toOpenGL() );
                statesCache.currentCombineMode_Alpha[ unit ] = ta.getCombineAlphaMode();
            }
            
            if ( !statesCache.enabled || statesCache.currentCombineSource0_RGB[ unit ] != ta.getCombineRGBSource( 0 ) )
            {
                GL11.glTexEnvf( GL11.GL_TEXTURE_ENV, EXTTextureEnvCombine.GL_SOURCE0_RGB_EXT, ta.getCombineRGBSource( 0 ).toOpenGL() );
                statesCache.currentCombineSource0_RGB[ unit ] = ta.getCombineRGBSource( 0 );
            }
            if ( !statesCache.enabled || statesCache.currentCombineSource0_Alpha[ unit ] != ta.getCombineAlphaSource( 0 ) )
            {
                GL11.glTexEnvf( GL11.GL_TEXTURE_ENV, EXTTextureEnvCombine.GL_SOURCE0_ALPHA_EXT, ta.getCombineAlphaSource( 0 ).toOpenGL() );
                statesCache.currentCombineSource0_Alpha[ unit ] = ta.getCombineAlphaSource( 0 );
            }
            
            if ( !statesCache.enabled || statesCache.currentCombineFunction0_RGB[ unit ] != ta.getCombineRGBFunction( 0 ) )
            {
                GL11.glTexEnvf( GL11.GL_TEXTURE_ENV, EXTTextureEnvCombine.GL_OPERAND0_RGB_EXT, ta.getCombineRGBFunction( 0 ).toOpenGL() );
                statesCache.currentCombineFunction0_RGB[ unit ] = ta.getCombineRGBFunction( 0 );
            }
            if ( !statesCache.enabled || statesCache.currentCombineFunction0_Alpha[ unit ] != ta.getCombineAlphaFunction( 0 ) )
            {
                GL11.glTexEnvf( GL11.GL_TEXTURE_ENV, EXTTextureEnvCombine.GL_OPERAND0_ALPHA_EXT, ta.getCombineAlphaFunction( 0 ).toOpenGL() );
                statesCache.currentCombineFunction0_Alpha[ unit ] = ta.getCombineAlphaFunction( 0 );
            }
            
            if ( !statesCache.enabled || statesCache.currentCombineSource1_RGB[ unit ] != ta.getCombineRGBSource( 1 ) )
            {
                GL11.glTexEnvf( GL11.GL_TEXTURE_ENV, EXTTextureEnvCombine.GL_SOURCE1_RGB_EXT, ta.getCombineRGBSource( 1 ).toOpenGL() );
                statesCache.currentCombineSource1_RGB[ unit ] = ta.getCombineRGBSource( 1 );
            }
            if ( !statesCache.enabled || statesCache.currentCombineSource1_Alpha[ unit ] != ta.getCombineAlphaSource( 1 ) )
            {
                GL11.glTexEnvf( GL11.GL_TEXTURE_ENV, EXTTextureEnvCombine.GL_SOURCE1_ALPHA_EXT, ta.getCombineAlphaSource( 1 ).toOpenGL() );
                statesCache.currentCombineSource1_Alpha[ unit ] = ta.getCombineAlphaSource( 1 );
            }
            
            if ( !statesCache.enabled || statesCache.currentCombineFunction1_RGB[ unit ] != ta.getCombineRGBFunction( 1 ) )
            {
                GL11.glTexEnvf( GL11.GL_TEXTURE_ENV, EXTTextureEnvCombine.GL_OPERAND1_RGB_EXT, ta.getCombineRGBFunction( 1 ).toOpenGL() );
                statesCache.currentCombineFunction1_RGB[ unit ] = ta.getCombineRGBFunction( 1 );
            }
            if ( !statesCache.enabled || statesCache.currentCombineFunction1_Alpha[ unit ] != ta.getCombineAlphaFunction( 1 ) )
            {
                GL11.glTexEnvf( GL11.GL_TEXTURE_ENV, EXTTextureEnvCombine.GL_OPERAND1_ALPHA_EXT, ta.getCombineAlphaFunction( 1 ).toOpenGL() );
                statesCache.currentCombineFunction1_Alpha[ unit ] = ta.getCombineAlphaFunction( 1 );
            }
            
            if ( !statesCache.enabled || statesCache.currentCombineSource2_RGB[ unit ] != ta.getCombineRGBSource( 2 ) )
            {
                GL11.glTexEnvf( GL11.GL_TEXTURE_ENV, ARBTextureEnvCombine.GL_SOURCE2_RGB_ARB, ta.getCombineRGBSource( 2 ).toOpenGL() );
                statesCache.currentCombineSource2_RGB[ unit ] = ta.getCombineRGBSource( 2 );
            }
            if ( !statesCache.enabled || statesCache.currentCombineSource2_Alpha[ unit ] != ta.getCombineAlphaSource( 2 ) )
            {
                GL11.glTexEnvf( GL11.GL_TEXTURE_ENV, ARBTextureEnvCombine.GL_SOURCE2_ALPHA_ARB, ta.getCombineAlphaSource( 2 ).toOpenGL() );
                statesCache.currentCombineSource2_Alpha[ unit ] = ta.getCombineAlphaSource( 2 );
            }
            
            if ( !statesCache.enabled || statesCache.currentCombineFunction2_RGB[ unit ] != ta.getCombineRGBFunction( 2 ) )
            {
                GL11.glTexEnvf( GL11.GL_TEXTURE_ENV, ARBTextureEnvCombine.GL_OPERAND2_RGB_ARB, ta.getCombineRGBFunction( 2 ).toOpenGL() );
                statesCache.currentCombineFunction2_RGB[ unit ] = ta.getCombineRGBFunction( 2 );
            }
            if ( !statesCache.enabled || statesCache.currentCombineFunction2_Alpha[ unit ] != ta.getCombineAlphaFunction( 2 ) )
            {
                GL11.glTexEnvf( GL11.GL_TEXTURE_ENV, ARBTextureEnvCombine.GL_OPERAND2_ALPHA_ARB, ta.getCombineAlphaFunction( 2 ).toOpenGL() );
                statesCache.currentCombineFunction2_Alpha[ unit ] = ta.getCombineAlphaFunction( 2 );
            }
            
            if ( !statesCache.enabled || statesCache.currentCombineRGBScale[ unit ] != ta.getCombineRGBScale() )
            {
                GL11.glTexEnvi( GL11.GL_TEXTURE_ENV, EXTTextureEnvCombine.GL_RGB_SCALE_EXT, ta.getCombineRGBScale() );
                statesCache.currentCombineRGBScale[ unit ] = ta.getCombineRGBScale();
            }
        }
        
        if ( ( texType == TextureType.TEXTURE_2D ) || ( texType == TextureType.TEXTURE_1D ) )
        {
            final int glTexType = texType.toOpenGL();
            
            if ( !statesCache.enabled || statesCache.currentCompareMode[ unit ] != ta.getCompareMode() )
            {
                setCompareMode( glInfo, glTexType, ta.getCompareMode() );
                statesCache.currentCompareMode[ unit ] = ta.getCompareMode();
            }
            
            if ( !statesCache.enabled || statesCache.currentCompareFunc[ unit ] != ta.getCompareFunction() )
            {
                setCompareFunc( glInfo, glTexType, ta.getCompareFunction() );
                statesCache.currentCompareFunc[ unit ] = ta.getCompareFunction();
            }
        }
    }
    
    private final void updateProjectiveTexture( Shape3D shape, int tuIndex, CanvasPeerImplBase canvas, long frameId )
    {
        final Appearance app = shape.getAppearance();
        
        if ( app == null )
            return;
        
        final TextureUnit tu = app.getTextureUnit( tuIndex );
        
        if ( !( tu instanceof ProjectiveTextureUnit ) )
            return;
        
        final ProjectiveTextureUnit projTU = (ProjectiveTextureUnit)tu;
        
        final float viewportAspect;
        if ( canvas.getCurrentViewport() == null )
            viewportAspect = canvas.getDisplayMode().getAspect();
        else
            viewportAspect = canvas.getCurrentViewport().getAspect();
        
        projTU.update( viewportAspect, frameId );
    }
    
    private static final void setupTextureCoordsGeneration( TexCoordGeneration texGen )
    {
        final int glGenMode = texGen.getGenMode().toOpenGL();
        int glPlaneName = GL11.GL_EYE_PLANE;
        FloatBuffer planeBuf = null;
        
        switch ( texGen.getGenMode() )
        {
            case OBJECT_LINEAR:
                glPlaneName = GL11.GL_OBJECT_PLANE;
                planeBuf = tmpPlaneBuffer;
                break;
            case EYE_LINEAR:
                glPlaneName = GL11.GL_EYE_PLANE;
                planeBuf = tmpPlaneBuffer;
                break;
            case SPHERE_MAP:
                break;
            case NORMAL_MAP:
                break;
            case REFLECTION_MAP:
                break;
            default:
                throw new Error( "Unsupported TExture-generation-mode " + texGen.getGenMode() );
        }
        
        switch ( texGen.getFormat() )
        {
            case TEXTURE_COORDINATES_1:
                GL11.glTexGeni( GL11.GL_S, GL11.GL_TEXTURE_GEN_MODE, glGenMode );
                if ( planeBuf != null )
                {
                    texGen.getPlaneS().writeToBuffer( planeBuf, true, true );
                    GL11.glTexGen( GL11.GL_S, glPlaneName, planeBuf );
                }
                break;
            case TEXTURE_COORDINATES_2:
                GL11.glTexGeni( GL11.GL_S, GL11.GL_TEXTURE_GEN_MODE, glGenMode );
                GL11.glTexGeni( GL11.GL_T, GL11.GL_TEXTURE_GEN_MODE, glGenMode );
                if ( planeBuf != null )
                {
                    texGen.getPlaneS().writeToBuffer( planeBuf, true, true );
                    GL11.glTexGen( GL11.GL_S, glPlaneName, planeBuf );
                    
                    texGen.getPlaneT().writeToBuffer( planeBuf, true, true );
                    GL11.glTexGen( GL11.GL_T, glPlaneName, planeBuf );
                }
                break;
            case TEXTURE_COORDINATES_3:
                GL11.glTexGeni( GL11.GL_S, GL11.GL_TEXTURE_GEN_MODE, glGenMode );
                GL11.glTexGeni( GL11.GL_T, GL11.GL_TEXTURE_GEN_MODE, glGenMode );
                GL11.glTexGeni( GL11.GL_R, GL11.GL_TEXTURE_GEN_MODE, glGenMode );
                if ( planeBuf != null )
                {
                    texGen.getPlaneS().writeToBuffer( planeBuf, true, true );
                    GL11.glTexGen( GL11.GL_S, glPlaneName, planeBuf );
                    
                    texGen.getPlaneT().writeToBuffer( planeBuf, true, true );
                    GL11.glTexGen( GL11.GL_T, glPlaneName, planeBuf );
                    
                    texGen.getPlaneR().writeToBuffer( planeBuf, true, true );
                    GL11.glTexGen( GL11.GL_R, glPlaneName, planeBuf );
                }
                break;
            case TEXTURE_COORDINATES_4:
                GL11.glTexGeni( GL11.GL_S, GL11.GL_TEXTURE_GEN_MODE, glGenMode );
                GL11.glTexGeni( GL11.GL_T, GL11.GL_TEXTURE_GEN_MODE, glGenMode );
                GL11.glTexGeni( GL11.GL_R, GL11.GL_TEXTURE_GEN_MODE, glGenMode );
                GL11.glTexGeni( GL11.GL_Q, GL11.GL_TEXTURE_GEN_MODE, glGenMode );
                if ( planeBuf != null )
                {
                    texGen.getPlaneS().writeToBuffer( planeBuf, true, true );
                    GL11.glTexGen( GL11.GL_S, glPlaneName, planeBuf );
                    
                    texGen.getPlaneT().writeToBuffer( planeBuf, true, true );
                    GL11.glTexGen( GL11.GL_T, glPlaneName, planeBuf );
                    
                    texGen.getPlaneR().writeToBuffer( planeBuf, true, true );
                    GL11.glTexGen( GL11.GL_R, glPlaneName, planeBuf );
                    
                    texGen.getPlaneQ().writeToBuffer( planeBuf, true, true );
                    GL11.glTexGen( GL11.GL_Q, glPlaneName, planeBuf );
                }
                break;
        }
    }
    
    private static final void applyTexCoordGenStates( int unit, int statesMask, OpenGLStatesCache statesCache )
    {
        if ( ( statesMask & 1 ) != 0 )
        {
            if ( !statesCache.enabled || ( statesCache.texGenEnableMask[ unit ] & 1 ) == 0 )
            {
                GL11.glEnable( GL11.GL_TEXTURE_GEN_S );
                statesCache.texGenEnableMask[ unit ] |= 1;
            }
        }
        else if ( !statesCache.enabled || ( statesCache.texGenEnableMask[ unit ] & 1 ) != 0 )
        {
            GL11.glDisable( GL11.GL_TEXTURE_GEN_S );
            statesCache.texGenEnableMask[ unit ] &= ~1;
        }
        
        if ( ( statesMask & 2 ) != 0 )
        {
            if ( !statesCache.enabled || ( statesCache.texGenEnableMask[ unit ] & 2 ) == 0 )
            {
                GL11.glEnable( GL11.GL_TEXTURE_GEN_T );
                statesCache.texGenEnableMask[ unit ] |= 2;
            }
        }
        else if ( !statesCache.enabled || ( statesCache.texGenEnableMask[ unit ] & 2 ) != 0 )
        {
            GL11.glDisable( GL11.GL_TEXTURE_GEN_T );
            statesCache.texGenEnableMask[ unit ] &= ~2;
        }
        
        if ( ( statesMask & 4 ) != 0 )
        {
            if ( !statesCache.enabled || ( statesCache.texGenEnableMask[ unit ] & 4 ) == 0 )
            {
                GL11.glEnable( GL11.GL_TEXTURE_GEN_R );
                statesCache.texGenEnableMask[ unit ] |= 4;
            }
        }
        else if ( !statesCache.enabled || ( statesCache.texGenEnableMask[ unit ] & 4 ) != 0 )
        {
            GL11.glDisable( GL11.GL_TEXTURE_GEN_R );
            statesCache.texGenEnableMask[ unit ] &= ~4;
        }
        
        if ( ( statesMask & 8 ) != 0 )
        {
            if ( !statesCache.enabled || ( statesCache.texGenEnableMask[ unit ] & 8 ) == 0 )
            {
                GL11.glEnable( GL11.GL_TEXTURE_GEN_Q );
                statesCache.texGenEnableMask[ unit ] |= 8;
            }
        }
        else if ( !statesCache.enabled || ( statesCache.texGenEnableMask[ unit ] & 8 ) != 0 )
        {
            GL11.glDisable( GL11.GL_TEXTURE_GEN_Q );
            statesCache.texGenEnableMask[ unit ] &= ~8;
        }
    }
    
    public void apply( RenderAtom< ? > atom, StateUnit stateUnit, Object glObj, CanvasPeer canvasPeer, RenderPeer renderPeer, OpenGLCapabilities glCaps, View view, OpenGLStatesCache statesCache, RenderOptions options, long nanoTime, long nanoStep, RenderMode renderMode, long frameId )
    {
        if ( !options.isTextureMappingEnabled() || ( renderMode != RenderMode.NORMAL ) )
        {
            return;
        }
        
        ProfileTimer.startProfile( X3DLog.LOG_CHANNEL, "TextureUnitStateUnitPeer::apply()" );
        
        
        final TextureUnitStateUnit texStateUnit = (TextureUnitStateUnit)stateUnit;
        final int unit = texStateUnit.getUnit();
        
        
        // (yvg) We should not jump out of allowed number of texture units,
        // because of this causes invalid behavior on some drivers
        if ( unit >= glCaps.getMaxTextureUnits() )
        {
            ProfileTimer.endProfile();
            return;
        }
        
        final Texture texture = texStateUnit.getTexture();
        
        if ( texture == null )
        {
            disableTextureState( statesCache, texture, unit, false );
            statesCache.currentBoundTexture[ unit ] = texture;
            
            ProfileTimer.endProfile();
            return;
        }
        else if ( !texture.isEnabled() )
        {
            disableTextureState( statesCache, texture, unit, texture.isChanged2() );
            statesCache.currentBoundTexture[ unit ] = texture;
            
            ProfileTimer.endProfile();
            return;
        }
        
        boolean changed = texture.isChanged2();
        if ( ( ( texture instanceof Texture2D ) && ( ( ( (Texture2D)texture ).hasTextureCanvas() && _SG_PrivilegedAccess.notifyDrawCallbacks( ( (Texture2D)texture ).getTextureCanvas(), nanoTime ) ) || ( (Texture2D)texture ).hasUpdateList() ) ) || ( !statesCache.enabled || statesCache.currentBoundTexture[ unit ] != texture ) || changed )
        {
            selectServerTextureUnit( unit, statesCache, false );
            
            // if this is multi texture then shape atom will handle it
            setTextureState( glCaps, statesCache, texture, unit, changed, canvasPeer, canvasPeer.getDepthBufferSize() );
            
            statesCache.currentBoundTexture[ unit ] = texture;
            //if ( texture != null )
            //    _SG_PrivilegedAccess.setChanged( texture, false );
        }
        
        
        final TextureAttributes texAttribs = texStateUnit.getTextureAttributes();
        
        changed = texAttribs.isChanged();
        boolean hasProjectiveTex = ( atom.getNode() instanceof Shape3D );
        if ( hasProjectiveTex )
        {
            Appearance app = ( (Shape3D)atom.getNode() ).getAppearance();
            
            if ( app == null )
                hasProjectiveTex = false;
            else
                hasProjectiveTex = ( app.getTextureUnit( unit ) instanceof ProjectiveTextureUnit );
        }
        
        //if ( ( !statesCache.enabled || statesCache.currentTexAttribs[ unit ] != texAttribs ) || changed || hasProjectiveTex )
        {
            if ( hasProjectiveTex )
            {
                updateProjectiveTexture( (Shape3D)atom.getNode(), unit, (CanvasPeerImplBase)renderPeer.getCanvasPeer(), frameId );
            }
            
            selectServerTextureUnit( unit, statesCache, false );
            
            setTextureAttributes( unit, texture.getType(), texAttribs, renderPeer.getCanvasPeer().getOpenGLInfo(), statesCache );
            
            statesCache.currentTexAttribs[ unit ] = texAttribs;
            if ( changed )
                _SG_PrivilegedAccess.setChanged( texAttribs, false );
        }
        
        
        final TexCoordGeneration texCoordGen = texStateUnit.getTexCoordGeneration();
        
        changed = texCoordGen.isChanged();
        //if ( ( !statesCache.enabled || statesCache.currentTexCoordGen[ unit ] != texCoordGen ) || changed )
        {
            if ( /*( texCoordGen != null ) && */texCoordGen.isEnabled() )
            {
                ShapeAtomPeer.setMatrix( view, atom.getNode().getWorldTransform(), atom.getNode().isBillboard(), false );
                
                selectServerTextureUnit( unit, statesCache, false );
                
                setupTextureCoordsGeneration( texCoordGen );
                
                final int texGenMask = texCoordGen.getFormat().getBitMask();
                
                if ( !statesCache.enabled || statesCache.texGenEnableMask[ unit ] != texGenMask )
                {
                    applyTexCoordGenStates( unit, texGenMask, statesCache );
                }
            }
            else
            {
                if ( !statesCache.enabled || statesCache.texGenEnableMask[ unit ] != 0 )
                {
                    selectServerTextureUnit( unit, statesCache, false );
                    
                    applyTexCoordGenStates( unit, 0, statesCache );
                }
            }
            
            statesCache.currentTexCoordGen[ unit ] = texCoordGen;
            if ( changed )
                _SG_PrivilegedAccess.setChanged( texCoordGen, false );
        }
        
        
        ProfileTimer.endProfile();
    }
}
