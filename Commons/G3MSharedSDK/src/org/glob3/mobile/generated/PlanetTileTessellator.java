package org.glob3.mobile.generated; 
public class PlanetTileTessellator extends TileTessellator
{
  private final boolean _skirted;
  private final Sector _renderedSector ;

  private Vector2I calculateResolution(Vector2I resolution, Tile tile, Sector renderedSector)
  {
    Sector sector = tile.getSector();
  
    final double latRatio = sector.getDeltaLatitude()._degrees / renderedSector.getDeltaLatitude()._degrees;
    final double lonRatio = sector.getDeltaLongitude()._degrees / renderedSector.getDeltaLongitude()._degrees;
  
    final IMathUtils mu = IMathUtils.instance();
  
    int resX = (int) mu.ceil((resolution._x / lonRatio));
    if (resX < 2)
    {
      resX = 2;
    }
  
    int resY = (int) mu.ceil((resolution._y / latRatio));
    if (resY < 2)
    {
      resY = 2;
    }
  
    final Vector2I meshRes = new Vector2I(resX, resY);
    return meshRes;
  
  
    //  return rawResolution;
  
    //  /* testing for dynamic latitude-resolution */
    //  const double cos = sector._center._latitude.cosinus();
    //
    //  int resolutionY = (int) (rawResolution._y * cos);
    //  if (resolutionY < 8) {
    //    resolutionY = 8;
    //  }
    //
    //  int resolutionX = (int) (rawResolution._x * cos);
    //  if (resolutionX < 8) {
    //    resolutionX = 8;
    //  }
    //
    //  return Vector2I(resolutionX, resolutionY);
  }

//C++ TO JAVA CONVERTER TODO TASK: The implementation of the following method could not be found:
//  Geodetic3D getGeodeticOnPlanetSurface(IMathUtils mu, Planet planet, ElevationData elevationData, float verticalExaggeration, Geodetic2D g);

  private boolean needsEastSkirt(Sector tileSector)
  {
    return _renderedSector.upperLongitude().greaterThan(tileSector.upperLongitude());
  }

  private boolean needsNorthSkirt(Sector tileSector)
  {
    return _renderedSector.upperLatitude().greaterThan(tileSector.upperLatitude());
  }

  private boolean needsWestSkirt(Sector tileSector)
  {
    return _renderedSector.lowerLongitude().lowerThan(tileSector.lowerLongitude());
  }

  private boolean needsSouthSkirt(Sector tileSector)
  {
    return _renderedSector.lowerLatitude().lowerThan(tileSector.lowerLatitude());
  }

  private Sector getRenderedSectorForTile(Tile tile)
  {
    return tile.getSector().intersection(_renderedSector);
  }

//  double getHeight(const Geodetic2D& g, const ElevationData* elevationData, double verticalExaggeration) const;


  //double PlanetTileTessellator::getHeight(const Geodetic2D& g, const ElevationData* elevationData, double verticalExaggeration) const{
  //  if (elevationData == NULL){
  //    return 0;
  //  }
  //  const double h = elevationData->getElevationAt(g);
  //  if (IMathUtils::instance()->isNan(h)){
  //    return 0;
  //  }
  //
  //  return h;
  //}
  
  private double createSurface(Sector tileSector, Sector meshSector, Vector2I meshResolution, ElevationData elevationData, float verticalExaggeration, boolean mercator, FloatBufferBuilderFromGeodetic vertices, ShortBufferBuilder indices, FloatBufferBuilderFromCartesian2D textCoords)
  {
  
    final int rx = meshResolution._x;
    final int ry = meshResolution._y;
  
    //CREATING TEXTURE COORDS////////////////////////////////////////////////////////////////
    final double mercatorLowerGlobalV = MercatorUtils.getMercatorV(tileSector._lower._latitude);
    final double mercatorUpperGlobalV = MercatorUtils.getMercatorV(tileSector._upper._latitude);
    final double mercatorDeltaGlobalV = mercatorLowerGlobalV - mercatorUpperGlobalV;
  
    //VERTICES///////////////////////////////////////////////////////////////
    final IMathUtils mu = IMathUtils.instance();
    double minElevation = 0;
    for (int j = 0; j < ry; j++)
    {
      final double v = (double) j / (ry - 1);
  
      for (int i = 0; i < rx; i++)
      {
        final double u = (double) i / (rx - 1);
        final Geodetic2D position = meshSector.getInnerPoint(u, v);
        double elevation = 0;
  
        if (elevationData != null)
        {
          final double rawElevation = elevationData.getElevationAt(position);
          if (!mu.isNan(rawElevation))
          {
            elevation = rawElevation * verticalExaggeration;
  
            if (elevation < minElevation)
            {
              minElevation = elevation;
            }
          }
        }
        vertices.add(position, elevation);
  
        //TEXT COORDS
        if (mercator)
        {
          //U
          final double m_u = tileSector.getUCoordinate(position._longitude);
  
          //V
          final double mercatorGlobalV = MercatorUtils.getMercatorV(position._latitude);
          final double m_v = (mercatorGlobalV - mercatorUpperGlobalV) / mercatorDeltaGlobalV;
  
          textCoords.add((float)m_u, (float)m_v);
        }
        else
        {
  
          Vector2D uv = tileSector.getUVCoordinates(position);
          textCoords.add(uv);
        }
      }
    }
  
    //INDEX///////////////////////////////////////////////////////////////
    for (short j = 0; j < (meshResolution._y-1); j++)
    {
      final short jTimesResolution = (short)(j *meshResolution._x);
      if (j > 0)
      {
        indices.add(jTimesResolution);
      }
      for (short i = 0; i < meshResolution._x; i++)
      {
        indices.add((short)(jTimesResolution + i));
        indices.add((short)(jTimesResolution + i + meshResolution._x));
      }
      indices.add((short)(jTimesResolution + 2 *meshResolution._x - 1));
    }
  
    return minElevation;
  }

  private void createEastSkirt(Planet planet, Sector tileSector, Sector meshSector, Vector2I meshResolution, double skirtHeight, FloatBufferBuilderFromGeodetic vertices, ShortBufferBuilder indices, FloatBufferBuilderFromCartesian2D textCoords)
  {
  
    //VERTICES///////////////////////////////////////////////////////////////
    final short firstSkirtVertex = (short)(vertices.size() / 3);
  
    final short rx = (short) meshResolution._x;
    final short ry = (short) meshResolution._y;
  
    final short southEastCorner = (short)((rx * ry) - 1);
  
    short skirtIndex = firstSkirtVertex;
    short surfaceIndex = southEastCorner;
  
    // east side
    for (int j = ry-1; j >= 0; j--)
    {
      final double x = 1;
      final double y = (double)j/(ry-1);
      final Geodetic2D g = meshSector.getInnerPoint(x, y);
      vertices.add(g, skirtHeight);
  
      //TEXTURE COORDS/////////////////////////////
      Vector2D uv = textCoords.getVector2D(surfaceIndex);
      textCoords.add(uv);
  
      //INDEX///////////////////////////////////////////////////////////////
      indices.add(surfaceIndex);
      indices.add(skirtIndex);
  
      skirtIndex++;
      surfaceIndex -= rx;
    }
  
    indices.add((short)(surfaceIndex + rx));
    indices.add((short)(surfaceIndex + rx));
  }

  private void createNorthSkirt(Planet planet, Sector tileSector, Sector meshSector, Vector2I meshResolution, double skirtHeight, FloatBufferBuilderFromGeodetic vertices, ShortBufferBuilder indices, FloatBufferBuilderFromCartesian2D textCoords)
  {
  
    //VERTICES///////////////////////////////////////////////////////////////
    final short firstSkirtVertex = (short)(vertices.size() / 3);
  
    final short rx = (short) meshResolution._x;
    //  const short ry = (short) meshResolution._y;
  
    final short northEastCorner = (short)(rx - 1);
  
    short skirtIndex = firstSkirtVertex;
    short surfaceIndex = northEastCorner;
  
    indices.add(surfaceIndex);
  
    for (int i = rx-1; i >= 0; i--)
    {
      final double x = (double)i/(rx-1);
      final double y = 0;
      final Geodetic2D g = meshSector.getInnerPoint(x, y);
      vertices.add(g, skirtHeight);
  
      //TEXTURE COORDS/////////////////////////////
      Vector2D uv = textCoords.getVector2D(surfaceIndex);
      textCoords.add(uv);
  
      //INDEX///////////////////////////////////////////////////////////////
      indices.add(surfaceIndex);
      indices.add(skirtIndex);
  
      skirtIndex++;
      surfaceIndex -= 1;
    }
  
    indices.add((short)(surfaceIndex + 1));
    indices.add((short)(surfaceIndex + 1));
  }

  private void createWestSkirt(Planet planet, Sector tileSector, Sector meshSector, Vector2I meshResolution, double skirtHeight, FloatBufferBuilderFromGeodetic vertices, ShortBufferBuilder indices, FloatBufferBuilderFromCartesian2D textCoords)
  {
  
    //VERTICES///////////////////////////////////////////////////////////////
    final short firstSkirtVertex = (short)(vertices.size() / 3);
  
    final short rx = (short) meshResolution._x;
    final short ry = (short) meshResolution._y;
  
    final short northWestCorner = (short)0;
  
    short skirtIndex = firstSkirtVertex;
    short surfaceIndex = northWestCorner;
  
    indices.add(surfaceIndex);
  
    for (int j = 0; j < ry; j++)
    {
      final double x = 0;
      final double y = (double)j/(ry-1);
      final Geodetic2D g = meshSector.getInnerPoint(x, y);
      vertices.add(g, skirtHeight);
  
      //TEXTURE COORDS/////////////////////////////
      Vector2D uv = textCoords.getVector2D(surfaceIndex);
      textCoords.add(uv);
  
      //INDEX///////////////////////////////////////////////////////////////
      indices.add(surfaceIndex);
      indices.add(skirtIndex);
  
      skirtIndex++;
      surfaceIndex += rx;
    }
  
    indices.add((short)(surfaceIndex - rx));
    indices.add((short)(surfaceIndex - rx));
  }

  private void createSouthSkirt(Planet planet, Sector tileSector, Sector meshSector, Vector2I meshResolution, double skirtHeight, FloatBufferBuilderFromGeodetic vertices, ShortBufferBuilder indices, FloatBufferBuilderFromCartesian2D textCoords)
  {
  
    //VERTICES///////////////////////////////////////////////////////////////
    final short firstSkirtVertex = (short)(vertices.size() / 3);
  
    final short rx = (short) meshResolution._x;
    final short ry = (short) meshResolution._y;
  
    final short southWestCorner = (short)(rx * (ry-1));
  
    short skirtIndex = firstSkirtVertex;
    short surfaceIndex = southWestCorner;
  
    indices.add(surfaceIndex);
  
    for (int i = 0; i < rx; i++)
    {
      final double x = (double)i/(rx-1);
      final double y = 1;
      final Geodetic2D g = meshSector.getInnerPoint(x, y);
      vertices.add(g, skirtHeight);
  
      //TEXTURE COORDS/////////////////////////////
      Vector2D uv = textCoords.getVector2D(surfaceIndex);
      textCoords.add((float)uv._x, (float)uv._y);
  
      //INDEX///////////////////////////////////////////////////////////////
      indices.add(surfaceIndex);
      indices.add((short) skirtIndex++);
      surfaceIndex += 1;
    }
  
    indices.add((short)(surfaceIndex - 1));
    indices.add((short)(surfaceIndex - 1));
  }


  public PlanetTileTessellator(boolean skirted, Sector sector)
  {
     _skirted = skirted;
     _renderedSector = new Sector(sector);
  }

  public void dispose()
  {
    super.dispose();
  
  }

  public final Vector2I getTileMeshResolution(Planet planet, Vector2I rawResolution, Tile tile, boolean debug)
  {
    Sector sector = getRenderedSectorForTile(tile); // tile->getSector();
    return calculateResolution(rawResolution, tile, sector);
  }


  public final Mesh createTileMesh(Planet planet, Vector2I rawResolution, Tile tile, ElevationData elevationData, float verticalExaggeration, boolean mercator, boolean renderDebug)
  {
  
    final Sector tileSector = tile.getSector();
    final Sector meshSector = getRenderedSectorForTile(tile); // tile->getSector();
    final Vector2I meshResolution = calculateResolution(rawResolution, tile, meshSector);
  
    FloatBufferBuilderFromGeodetic vertices = FloatBufferBuilderFromGeodetic.builderWithGivenCenter(planet, meshSector._center);
    ShortBufferBuilder indices = new ShortBufferBuilder();
    FloatBufferBuilderFromCartesian2D textCoords = new FloatBufferBuilderFromCartesian2D();
  
    double minElevation = createSurface(tileSector, meshSector, meshResolution, elevationData, verticalExaggeration, mercator, vertices, indices, textCoords);
  
    if (_skirted)
    {
  
      final Vector3D sw = planet.toCartesian(tileSector.getSW());
      final Vector3D nw = planet.toCartesian(tileSector.getNW());
      //    const double offset = nw.sub(sw).length() * 1e-3;
      final double relativeSkirtHeight = (nw.sub(sw).length() * 0.05 * -1) + minElevation;
      final double absoluteSkirtHeight = -1e5; //TODO: CHECK
  
      createEastSkirt(planet, tileSector, meshSector, meshResolution, needsEastSkirt(tileSector)? relativeSkirtHeight : absoluteSkirtHeight, vertices, indices, textCoords);
  
      createNorthSkirt(planet, tileSector, meshSector, meshResolution, needsNorthSkirt(tileSector)? relativeSkirtHeight : absoluteSkirtHeight, vertices, indices, textCoords);
  
      createWestSkirt(planet, tileSector, meshSector, meshResolution, needsWestSkirt(tileSector)? relativeSkirtHeight : absoluteSkirtHeight, vertices, indices, textCoords);
  
      createSouthSkirt(planet, tileSector, meshSector, meshResolution, needsSouthSkirt(tileSector)? relativeSkirtHeight : absoluteSkirtHeight, vertices, indices, textCoords);
    }
  
    //Storing textCoords in Tile
    tile.setTessellatorData(new PlanetTileTessellatorData(textCoords));
  
    return new IndexedGeometryMesh(GLPrimitive.triangleStrip(), vertices.getCenter(), vertices.create(), true, indices.create(), true, 1, 1);
  }

  public final Mesh createTileDebugMesh(Planet planet, Vector2I rawResolution, Tile tile)
  {
    final Sector sector = getRenderedSectorForTile(tile); // tile->getSector();
  
    final int resolutionXMinus1 = rawResolution._x - 1;
    final int resolutionYMinus1 = rawResolution._y - 1;
    short posS = 0;
  
    // compute offset for vertices
    final Vector3D sw = planet.toCartesian(sector.getSW());
    final Vector3D nw = planet.toCartesian(sector.getNW());
    final double offset = nw.sub(sw).length() * 1e-3;
  
    FloatBufferBuilderFromGeodetic vertices = FloatBufferBuilderFromGeodetic.builderWithGivenCenter(planet, sector._center);
    ShortBufferBuilder indices = new ShortBufferBuilder();
  
    // west side
    for (int j = 0; j < resolutionYMinus1; j++)
    {
      vertices.add(sector.getInnerPoint(0, (double)j/resolutionYMinus1), offset);
      indices.add(posS++);
    }
  
    // south side
    for (int i = 0; i < resolutionXMinus1; i++)
    {
      vertices.add(sector.getInnerPoint((double)i/resolutionXMinus1, 1), offset);
      indices.add(posS++);
    }
  
    // east side
    for (int j = resolutionYMinus1; j > 0; j--)
    {
      vertices.add(sector.getInnerPoint(1, (double)j/resolutionYMinus1), offset);
      indices.add(posS++);
    }
  
    // north side
    for (int i = resolutionXMinus1; i > 0; i--)
    {
      vertices.add(sector.getInnerPoint((double)i/resolutionXMinus1, 0), offset);
      indices.add(posS++);
    }
  
    Color color = Color.newFromRGBA((float) 1.0, (float) 0.0, (float) 0, (float) 1.0);
  
    return new IndexedMesh(GLPrimitive.lineLoop(), true, vertices.getCenter(), vertices.create(), indices.create(), 1, 1, color, null, 0, false); // colorsIntensity -  colors
  }

  public final boolean isReady(G3MRenderContext rc)
  {
    return true;
  }

  public final IFloatBuffer createTextCoords(Vector2I rawResolution, Tile tile, boolean mercator)
  {
  
    PlanetTileTessellatorData data = (PlanetTileTessellatorData) tile.getTessellatorData();
    if (data == null || data._textCoords == null)
    {
      ILogger.instance().logError("Logic error on PlanetTileTessellator::createTextCoord");
      return null;
    }
    return data._textCoords.create();
  }

  public final Vector2D getTextCoord(Tile tile, Angle latitude, Angle longitude, boolean mercator)
  {
    final Sector sector = tile.getSector();
  
    final Vector2D linearUV = sector.getUVCoordinates(latitude, longitude);
    if (!mercator)
    {
      return linearUV;
    }
  
    final double lowerGlobalV = MercatorUtils.getMercatorV(sector._lower._latitude);
    final double upperGlobalV = MercatorUtils.getMercatorV(sector._upper._latitude);
    final double deltaGlobalV = lowerGlobalV - upperGlobalV;
  
    final double globalV = MercatorUtils.getMercatorV(latitude);
    final double localV = (globalV - upperGlobalV) / deltaGlobalV;
  
    return new Vector2D(linearUV._x, localV);
  }

}