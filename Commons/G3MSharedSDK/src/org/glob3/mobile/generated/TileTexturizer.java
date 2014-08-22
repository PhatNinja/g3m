package org.glob3.mobile.generated; 
//
//  TileTexturizer.cpp
//  G3MiOSSDK
//
//  Created by Diego Gomez Deck on 12/07/12.
//  Copyright (c) 2012 IGO Software SL. All rights reserved.
//

//
//  TileTexturizer.hpp
//  G3MiOSSDK
//
//  Created by Diego Gomez Deck on 12/07/12.
//  Copyright (c) 2012 IGO Software SL. All rights reserved.
//



//class Mesh;
//class G3MRenderContext;
//class Tile;
//class TileTessellator;
//class G3MContext;
//class TilesRenderParameters;
//class Geodetic3D;
//class LayerSet;
//class TileRasterizer;
//class LayerTilesRenderParameters;
//class G3MEventContext;


//C++ TO JAVA CONVERTER TODO TASK: There is no preprocessor in Java:
//#warning añadir el defaultImage (IImageBuilder que no acepte provider que sean mutables, así no hay que implementar el changedlistener)(forzarla a que esté disponible, si no no arranca el el globo), con las image hay que pasar shallowcopy, porque los listener van borrando la imagen.Transformar el tablero de ajedrez en un IImageBuilder.Como default el tablero de ajedrez (en el builder).
public abstract class TileTexturizer
{
  public void dispose()
  {
  }

  public abstract RenderState getRenderState(LayerSet layerSet);

  public abstract void initialize(G3MContext context, TilesRenderParameters parameters);

  public abstract Mesh texturize(G3MRenderContext rc, TileTessellator tessellator, LayerTilesRenderParameters layerTilesRenderParameters, LayerSet layerSet, boolean forceFullRender, long tileDownloadPriority, Tile tile, Mesh tessellatorMesh, Mesh previousMesh, boolean logTilesPetitions);
//                          TileRasterizer* tileRasterizer,

  public abstract void tileToBeDeleted(Tile tile, Mesh mesh);

  public abstract void tileMeshToBeDeleted(Tile tile, Mesh mesh);

  public abstract boolean tileMeetsRenderCriteria(Tile tile);

  public abstract void justCreatedTopTile(G3MRenderContext rc, Tile tile, LayerSet layerSet);

  public abstract void ancestorTexturedSolvedChanged(Tile tile, Tile ancestorTile, boolean textureSolved);

  public abstract boolean onTerrainTouchEvent(G3MEventContext ec, Geodetic3D position, Tile tile, LayerSet layerSet);

}