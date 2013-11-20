package org.glob3.mobile.generated; 
public class PlanetRenderer extends LeafRenderer implements ChangedListener, SurfaceElevationProvider
{
  private TileTessellator _tessellator;
  private ElevationDataProvider _elevationDataProvider;
  private boolean _ownsElevationDataProvider;
  private TileTexturizer _texturizer;
  private TileRasterizer _tileRasterizer;
  private LayerSet _layerSet;
  private final TilesRenderParameters _tilesRenderParameters;
  private final boolean _showStatistics;
  private ITileVisitor _tileVisitor = null;

  private TilesStatistics _statistics = new TilesStatistics();

  private Camera     _lastCamera;
  private G3MContext _context;

  private java.util.ArrayList<Tile> _firstLevelTiles = new java.util.ArrayList<Tile>();
  private boolean _firstLevelTilesJustCreated;
  private boolean _allFirstLevelTilesAreTextureSolved;

  private ITimer _lastSplitTimer; // timer to start every time a tile get splitted into subtiles

  private void clearFirstLevelTiles()
  {
    final int firstLevelTilesCount = _firstLevelTiles.size();
    for (int i = 0; i < firstLevelTilesCount; i++)
    {
      Tile tile = _firstLevelTiles.get(i);
  
      tile.toBeDeleted(_texturizer, _elevationDataProvider);
  
      if (tile != null)
         tile.dispose();
    }
  
    _firstLevelTiles.clear();
  }
  private void createFirstLevelTiles(G3MContext context)
  {
  
    final LayerTilesRenderParameters parameters = getLayerTilesRenderParameters();
    if (parameters == null)
    {
      //ILogger::instance()->logError("LayerSet returned a NULL for LayerTilesRenderParameters, can't create first-level tiles");
      return;
    }
  
    java.util.ArrayList<Tile> topLevelTiles = new java.util.ArrayList<Tile>();
  
    final Angle fromLatitude = parameters._topSector._lower._latitude;
    final Angle fromLongitude = parameters._topSector._lower._longitude;
  
    final Angle deltaLan = parameters._topSector._deltaLatitude;
    final Angle deltaLon = parameters._topSector._deltaLongitude;
  
    final int topSectorSplitsByLatitude = parameters._topSectorSplitsByLatitude;
    final int topSectorSplitsByLongitude = parameters._topSectorSplitsByLongitude;
  
    final Angle tileHeight = deltaLan.div(topSectorSplitsByLatitude);
    final Angle tileWidth = deltaLon.div(topSectorSplitsByLongitude);
  
    for (int row = 0; row < topSectorSplitsByLatitude; row++)
    {
      final Angle tileLatFrom = tileHeight.times(row).add(fromLatitude);
      final Angle tileLatTo = tileLatFrom.add(tileHeight);
  
      for (int col = 0; col < topSectorSplitsByLongitude; col++)
      {
        final Angle tileLonFrom = tileWidth.times(col).add(fromLongitude);
        final Angle tileLonTo = tileLonFrom.add(tileWidth);
  
        final Geodetic2D tileLower = new Geodetic2D(tileLatFrom, tileLonFrom);
        final Geodetic2D tileUpper = new Geodetic2D(tileLatTo, tileLonTo);
        final Sector sector = new Sector(tileLower, tileUpper);
  
        if (_renderedSector == null || sector.touchesWith(_renderedSector)) //Do not create innecesary tiles
        {
          Tile tile = new Tile(_texturizer, null, sector, 0, row, col, this);
          if (parameters._firstLevel == 0)
          {
            _firstLevelTiles.add(tile);
          }
          else
          {
            topLevelTiles.add(tile);
          }
        }
      }
    }
  
    if (parameters._firstLevel > 0)
    {
      final int topLevelTilesSize = topLevelTiles.size();
      for (int i = 0; i < topLevelTilesSize; i++)
      {
        Tile tile = topLevelTiles.get(i);
        createFirstLevelTiles(_firstLevelTiles, tile, parameters._firstLevel, parameters._mercator);
      }
    }
  
    sortTiles(_firstLevelTiles);
  
    context.getLogger().logInfo("Created %d first level tiles", _firstLevelTiles.size());
  
    _firstLevelTilesJustCreated = true;
  }
  private void createFirstLevelTiles(java.util.ArrayList<Tile> firstLevelTiles, Tile tile, int firstLevel, boolean mercator)
  {
    if (tile._level == firstLevel)
    {
      firstLevelTiles.add(tile);
    }
    else
    {
      final Sector sector = tile._sector;
      final Geodetic2D lower = sector._lower;
      final Geodetic2D upper = sector._upper;
  
      final Angle splitLongitude = Angle.midAngle(lower._longitude, upper._longitude);
  
      final Angle splitLatitude = mercator ? MercatorUtils.calculateSplitLatitude(lower._latitude, upper._latitude) : Angle.midAngle(lower._latitude, upper._latitude);
      /*                               */
      /*                               */
  
  
      java.util.ArrayList<Tile> children = tile.createSubTiles(splitLatitude, splitLongitude, false);
  
      final int childrenSize = children.size();
      for (int i = 0; i < childrenSize; i++)
      {
        Tile child = children.get(i);
        createFirstLevelTiles(firstLevelTiles, child, firstLevel, mercator);
      }
  
      children = null;
      if (tile != null)
         tile.dispose();
    }
  }

  private void sortTiles(java.util.ArrayList<Tile> tiles)
  {
    java.util.Collections.sort(tiles,
                               new java.util.Comparator<Tile>() {
                                 @Override
                                 public int compare(final Tile i,
                                                    final Tile j) {
                                   final int rowI = i._row;
                                   final int rowJ = j._row;
                                   if (rowI < rowJ) {
                                     return -1;
                                   }
                                   if (rowI > rowJ) {
                                     return 1;
                                   }
  
                                   final int columnI = i._column;
                                   final int columnJ = j._column;
                                   if (columnI < columnJ) {
                                     return -1;
                                   }
                                   if (columnI > columnJ) {
                                     return 1;
                                   }
                                   return 0;
                                 }
                               });
  }

  private boolean _firstRender;

  private void pruneFirstLevelTiles()
  {
    final int firstLevelTilesCount = _firstLevelTiles.size();
    for (int i = 0; i < firstLevelTilesCount; i++)
    {
      Tile tile = _firstLevelTiles.get(i);
      tile.prune(_texturizer, _elevationDataProvider);
    }
  }

  private Sector _lastVisibleSector;

  private java.util.ArrayList<VisibleSectorListenerEntry> _visibleSectorListeners = new java.util.ArrayList<VisibleSectorListenerEntry>();

  private void visitTilesTouchesWith(Sector sector, int firstLevel, int maxLevel)
  {
    if (_tileVisitor != null)
    {
      final LayerTilesRenderParameters parameters = getLayerTilesRenderParameters();
      if (parameters == null)
      {
        ILogger.instance().logError("LayerSet returned a NULL for LayerTilesRenderParameters, can't create first-level tiles");
        return;
      }
  
      final int firstLevelToVisit = (firstLevel < parameters._firstLevel) ? parameters._firstLevel : firstLevel;
      if (firstLevel < firstLevelToVisit)
      {
        ILogger.instance().logError("Can only visit from level %", firstLevelToVisit);
        return;
      }
  
      final int maxLevelToVisit = (maxLevel > parameters._maxLevel) ? parameters._maxLevel : maxLevel;
      if (maxLevel > maxLevelToVisit)
      {
        ILogger.instance().logError("Can only visit to level %", maxLevelToVisit);
        return;
      }
  
      if (firstLevelToVisit > maxLevelToVisit)
      {
        ILogger.instance().logError("Can't visit, first level is gratter than max level");
        return;
      }
  
      java.util.ArrayList<Layer> layers = new java.util.ArrayList<Layer>();
      final int layersCount = _layerSet.size();
      for (int i = 0; i < layersCount; i++)
      {
        Layer layer = _layerSet.getLayer(i);
        if (layer.isEnable() && layer.isReady())
        {
          layers.add(layer);
        }
      }
  
      final int firstLevelTilesCount = _firstLevelTiles.size();
      for (int i = 0; i < firstLevelTilesCount; i++)
      {
        Tile tile = _firstLevelTiles.get(i);
        if (tile._sector.touchesWith(sector))
        {
          _tileVisitor.visitTile(layers, tile);
          visitSubTilesTouchesWith(layers, tile, sector, firstLevelToVisit, maxLevelToVisit);
        }
      }
    }
    else
    {
      ILogger.instance().logError("TileVisitor is NULL");
    }
  }

  private void visitSubTilesTouchesWith(java.util.ArrayList<Layer> layers, Tile tile, Sector sectorToVisit, int topLevel, int maxLevel)
  {
    if (tile._level < maxLevel)
    {
      java.util.ArrayList<Tile> subTiles = tile.getSubTiles(getLayerTilesRenderParameters()._mercator);
  
      final int subTilesCount = subTiles.size();
      for (int i = 0; i < subTilesCount; i++)
      {
        Tile tl = subTiles.get(i);
        if (tl._sector.touchesWith(sectorToVisit))
        {
          if ((tile._level >= topLevel))
          {
            _tileVisitor.visitTile(layers, tl);
          }
          visitSubTilesTouchesWith(layers, tl, sectorToVisit, topLevel, maxLevel);
        }
      }
    }
  }

  private long _texturePriority;

  private float _verticalExaggeration;

  private boolean _recreateTilesPending;

  private GLState _glState;
  private void updateGLState(G3MRenderContext rc)
  {
  
    final Camera cam = rc.getCurrentCamera();
    ModelViewGLFeature f = (ModelViewGLFeature) _glState.getGLFeature(GLFeatureID.GLF_MODEL_VIEW);
    if (f == null)
    {
      _glState.addGLFeature(new ModelViewGLFeature(cam), true);
    }
    else
    {
      f.setMatrix(cam.getModelViewMatrix44D());
    }
  
  }

  private SurfaceElevationProvider_Tree _elevationListenersTree = new SurfaceElevationProvider_Tree();

  private Sector _renderedSector;
//  bool _validLayerTilesRenderParameters;
  private boolean _layerTilesRenderParametersDirty;
  private LayerTilesRenderParameters _layerTilesRenderParameters;
  private java.util.ArrayList<String> _errors = new java.util.ArrayList<String>();

  private LayerTilesRenderParameters getLayerTilesRenderParameters()
  {
    if (_layerTilesRenderParametersDirty)
    {
      _errors.clear();
      _layerTilesRenderParameters = null;
      _layerTilesRenderParameters = _layerSet.createLayerTilesRenderParameters(_errors);
      if (_layerTilesRenderParameters == null)
      {
        ILogger.instance().logError("LayerSet returned a NULL for LayerTilesRenderParameters, can't render planet");
      }
      _layerTilesRenderParametersDirty = false;
    }
    return _layerTilesRenderParameters;
  }

  private java.util.ArrayList<TerrainTouchListener> _terrainTouchListeners = new java.util.ArrayList<TerrainTouchListener>();

  public PlanetRenderer(TileTessellator tessellator, ElevationDataProvider elevationDataProvider, boolean ownsElevationDataProvider, float verticalExaggeration, TileTexturizer texturizer, TileRasterizer tileRasterizer, LayerSet layerSet, TilesRenderParameters tilesRenderParameters, boolean showStatistics, long texturePriority, Sector renderedSector)
  {
     _tessellator = tessellator;
     _elevationDataProvider = elevationDataProvider;
     _ownsElevationDataProvider = ownsElevationDataProvider;
     _verticalExaggeration = verticalExaggeration;
     _texturizer = texturizer;
     _tileRasterizer = tileRasterizer;
     _layerSet = layerSet;
     _tilesRenderParameters = tilesRenderParameters;
     _showStatistics = showStatistics;
     _firstLevelTilesJustCreated = false;
     _lastSplitTimer = null;
     _lastCamera = null;
     _firstRender = false;
     _context = null;
     _lastVisibleSector = null;
     _texturePriority = texturePriority;
     _allFirstLevelTilesAreTextureSolved = false;
     _recreateTilesPending = false;
     _glState = new GLState();
     _renderedSector = renderedSector.isEquals(Sector.fullSphere())? null : new Sector(renderedSector);
     _layerTilesRenderParameters = null;
     _layerTilesRenderParametersDirty = true;
    _layerSet.setChangeListener(this);
    if (_tileRasterizer != null)
    {
      _tileRasterizer.setChangeListener(this);
    }
  }

  public void dispose()
  {
    clearFirstLevelTiles();
  
    _layerTilesRenderParameters = null;
  
    if (_tessellator != null)
       _tessellator.dispose();
    if (_elevationDataProvider != null)
       _elevationDataProvider.dispose();
    if (_texturizer != null)
       _texturizer.dispose();
    if (_tilesRenderParameters != null)
       _tilesRenderParameters.dispose();
  
    if (_lastSplitTimer != null)
       _lastSplitTimer.dispose();
  
    if (_lastVisibleSector != null)
       _lastVisibleSector.dispose();
  
    final int visibleSectorListenersCount = _visibleSectorListeners.size();
    for (int i = 0; i < visibleSectorListenersCount; i++)
    {
      VisibleSectorListenerEntry entry = _visibleSectorListeners.get(i);
      if (entry != null)
         entry.dispose();
    }
  
    if (_renderedSector != null)
       _renderedSector.dispose();
  
    super.dispose();
  
  }

  public final void initialize(G3MContext context)
  {
    _context = context;
  
    clearFirstLevelTiles();
    createFirstLevelTiles(context);
  
    if (_lastSplitTimer != null)
       _lastSplitTimer.dispose();
    _lastSplitTimer = context.getFactory().createTimer();
  
    _layerSet.initialize(context);
    _texturizer.initialize(context, _tilesRenderParameters);
    if (_elevationDataProvider != null)
    {
      _elevationDataProvider.initialize(context);
    }
    if (_tileRasterizer != null)
    {
      _tileRasterizer.initialize(context);
    }
  }

  public final void render(G3MRenderContext rc, GLState glState)
  {
  
    final LayerTilesRenderParameters layerTilesRenderParameters = getLayerTilesRenderParameters();
    if (layerTilesRenderParameters == null)
    {
      return;
    }
  
    updateGLState(rc);
//C++ TO JAVA CONVERTER TODO TASK: There is no preprocessor in Java:
//#warning Testing_Terrain_Normals;
    _glState.setParent(glState);
  
    // Saving camera for use in onTouchEvent
    _lastCamera = rc.getCurrentCamera();
  
    _statistics.clear();
  
    final IDeviceInfo deviceInfo = IFactory.instance().getDeviceInfo();
    final float dpiFactor = deviceInfo.getPixelsInMM(0.1f);
    final float deviceQualityFactor = deviceInfo.getQualityFactor();
  
    final int firstLevelTilesCount = _firstLevelTiles.size();
  
    final Planet planet = rc.getPlanet();
    final Vector3D cameraNormalizedPosition = _lastCamera.getNormalizedPosition();
    double cameraAngle2HorizonInRadians = _lastCamera.getAngle2HorizonInRadians();
    final Frustum cameraFrustumInModelCoordinates = _lastCamera.getFrustumInModelCoordinates();
  
    if (_firstRender && _tilesRenderParameters._forceFirstLevelTilesRenderOnStart)
    {
      // force one render pass of the firstLevelTiles tiles to make the (toplevel) textures
      // loaded as they will be used as last-chance fallback texture for any tile.
      _firstRender = false;
  
      for (int i = 0; i < firstLevelTilesCount; i++)
      {
        Tile tile = _firstLevelTiles.get(i);
        tile.render(rc, _glState, null, planet, cameraNormalizedPosition, cameraAngle2HorizonInRadians, cameraFrustumInModelCoordinates, _statistics, _verticalExaggeration, layerTilesRenderParameters, _texturizer, _tilesRenderParameters, _lastSplitTimer, _elevationDataProvider, _tessellator, _tileRasterizer, _layerSet, _renderedSector, _firstRender, _texturePriority, dpiFactor, deviceQualityFactor); // if first render, force full render
      }
    }
    else
    {
      java.util.LinkedList<Tile> toVisit = new java.util.LinkedList<Tile>();
      for (int i = 0; i < firstLevelTilesCount; i++)
      {
        toVisit.addLast(_firstLevelTiles.get(i));
      }
  
      while (toVisit.size() > 0)
      {
        java.util.LinkedList<Tile> toVisitInNextIteration = new java.util.LinkedList<Tile>();
  
        for (java.util.Iterator<Tile> iter = toVisit.iterator(); iter.hasNext();)
        {
          Tile tile = iter.next();
  
          tile.render(rc, _glState, toVisitInNextIteration, planet, cameraNormalizedPosition, cameraAngle2HorizonInRadians, cameraFrustumInModelCoordinates, _statistics, _verticalExaggeration, layerTilesRenderParameters, _texturizer, _tilesRenderParameters, _lastSplitTimer, _elevationDataProvider, _tessellator, _tileRasterizer, _layerSet, _renderedSector, _firstRender, _texturePriority, dpiFactor, deviceQualityFactor); // if first render, force full render
        }
  
        toVisit = toVisitInNextIteration;
      }
    }
  
    if (_showStatistics)
    {
      _statistics.log(rc.getLogger());
    }
  
  
    final Sector renderedSector = _statistics.getRenderedSector();
    if (renderedSector != null)
    {
      if ((_lastVisibleSector == null) || !renderedSector.isEquals(_lastVisibleSector))
      {
        if (_lastVisibleSector != null)
           _lastVisibleSector.dispose();
        _lastVisibleSector = new Sector(renderedSector);
      }
    }
  
    if (_lastVisibleSector != null)
    {
      final int visibleSectorListenersCount = _visibleSectorListeners.size();
      for (int i = 0; i < visibleSectorListenersCount; i++)
      {
        VisibleSectorListenerEntry entry = _visibleSectorListeners.get(i);
  
        entry.tryToNotifyListener(_lastVisibleSector, rc);
      }
    }
  
  }

  public final boolean onTouchEvent(G3MEventContext ec, TouchEvent touchEvent)
  {
    if (_lastCamera == null)
    {
      return false;
    }
  
    if (touchEvent.getType() == TouchEventType.LongPress)
    {
      final Vector2I pixel = touchEvent.getTouch(0).getPos();
      final Vector3D ray = _lastCamera.pixel2Ray(pixel);
      final Vector3D origin = _lastCamera.getCartesianPosition();
  
      final Planet planet = ec.getPlanet();
  
      final Vector3D positionCartesian = planet.closestIntersection(origin, ray);
      if (positionCartesian.isNan())
      {
        return false;
      }
  
      final Geodetic3D position = planet.toGeodetic3D(positionCartesian);
  
      final int firstLevelTilesCount = _firstLevelTiles.size();
      for (int i = 0; i < firstLevelTilesCount; i++)
      {
        final Tile tile = _firstLevelTiles.get(i).getDeepestTileContaining(position);
        if (tile != null)
        {
          ILogger.instance().logInfo("Touched on %s", tile.description());
          ILogger.instance().logInfo("Camera position=%s heading=%f pitch=%f", _lastCamera.getGeodeticPosition().description(), _lastCamera.getHeading()._degrees, _lastCamera.getPitch()._degrees);
  
          if (_texturizer.onTerrainTouchEvent(ec, position, tile, _layerSet))
          {
            return true;
          }
  
          final int terrainTouchListenersSize = _terrainTouchListeners.size();
          for (int j = terrainTouchListenersSize-1; j >= 0; j--)
          {
            TerrainTouchListener listener = _terrainTouchListeners.get(j);
            if (listener.onTerrainTouch(ec, pixel, _lastCamera, position, tile))
            {
              return true;
            }
          }
  
          return false;
        }
      }
  
    }
  
    return false;
  }

  public final void onResizeViewportEvent(G3MEventContext ec, int width, int height)
  {

  }

  public final RenderState getRenderState(G3MRenderContext rc)
  {
    final LayerTilesRenderParameters layerTilesRenderParameters = getLayerTilesRenderParameters();
  
    if (layerTilesRenderParameters == null)
    {
      return RenderState.error(_errors);
    }
  
    if (!_layerSet.isReady())
    {
//C++ TO JAVA CONVERTER TODO TASK: There is no preprocessor in Java:
//#warning __TODO_Layer_error;
      return RenderState.busy();
    }
  
    if (_elevationDataProvider != null)
    {
      if (!_elevationDataProvider.isReadyToRender(rc))
      {
        return RenderState.busy();
      }
    }
  
    if (_firstLevelTilesJustCreated)
    {
      _firstLevelTilesJustCreated = false;
  
      final int firstLevelTilesCount = _firstLevelTiles.size();
  
      if (_tilesRenderParameters._forceFirstLevelTilesRenderOnStart)
      {
        _statistics.clear();
  
        for (int i = 0; i < firstLevelTilesCount; i++)
        {
          Tile tile = _firstLevelTiles.get(i);
          tile.prepareForFullRendering(rc, _texturizer, _elevationDataProvider, _tessellator, _tileRasterizer, layerTilesRenderParameters, _layerSet, _tilesRenderParameters, true, _texturePriority, _verticalExaggeration);
        }
      }
  
      if (_texturizer != null)
      {
        for (int i = 0; i < firstLevelTilesCount; i++)
        {
          Tile tile = _firstLevelTiles.get(i);
          _texturizer.justCreatedTopTile(rc, tile, _layerSet);
        }
      }
    }
  
    if (_tilesRenderParameters._forceFirstLevelTilesRenderOnStart)
    {
      if (!_allFirstLevelTilesAreTextureSolved)
      {
        final int firstLevelTilesCount = _firstLevelTiles.size();
        for (int i = 0; i < firstLevelTilesCount; i++)
        {
          Tile tile = _firstLevelTiles.get(i);
          if (!tile.isTextureSolved())
          {
            return RenderState.busy();
          }
        }
  
        if (_tessellator != null)
        {
          if (!_tessellator.isReady(rc))
          {
            return RenderState.busy();
          }
        }
  
        if (_texturizer != null)
        {
          if (!_texturizer.isReady(rc, _layerSet))
          {
            return RenderState.busy();
          }
        }
  
        _allFirstLevelTilesAreTextureSolved = true;
      }
    }
  
    return RenderState.ready();
  }

  public final void acceptTileVisitor(ITileVisitor tileVisitor, Sector sector, int topLevel, int maxLevel)
  {
    _tileVisitor = tileVisitor;
    visitTilesTouchesWith(sector, topLevel, maxLevel);
  }

  public final void start(G3MRenderContext rc)
  {
    _firstRender = true;
  }

  public final void stop(G3MRenderContext rc)
  {
    _firstRender = false;
  }

  public final void onResume(G3MContext context)
  {

  }

  public final void onPause(G3MContext context)
  {
    recreateTiles();
  }

  public final void onDestroy(G3MContext context)
  {

  }

  public final void setEnable(boolean enable)
  {
    super.setEnable(enable);

    if (!enable)
    {
      pruneFirstLevelTiles();
    }
  }

  public final void changed()
  {
    if (!_recreateTilesPending)
    {
      _recreateTilesPending = true;
      // recreateTiles() delete tiles, then meshes, and delete textures from the GPU
      //   so it has to be executed in the OpenGL thread
      _context.getThreadUtils().invokeInRendererThread(new RecreateTilesTask(this), true);
    }
  }

  public final void recreateTiles()
  {
    pruneFirstLevelTiles();
    clearFirstLevelTiles();
  
    _layerTilesRenderParameters = null;
    _layerTilesRenderParameters = null;
    _layerTilesRenderParametersDirty = true;
  
    _firstRender = true;
    _allFirstLevelTilesAreTextureSolved = false;
    createFirstLevelTiles(_context);
  
    _recreateTilesPending = false;
  }

  /**
   Answer the visible-sector, it can be null if globe was not yet rendered.
   */
  public final Sector getVisibleSector()
  {
    return _lastVisibleSector;
  }

  /**
   Add a listener for notification of visible-sector changes.

   @param stabilizationInterval How many time the visible-sector has to be settled (without changes) before triggering the event.  Useful for avoid process while the camera is being moved (as in animations).  If stabilizationInterval is zero, the event is triggered inmediatly.
   */
  public final void addVisibleSectorListener(VisibleSectorListener listener, TimeInterval stabilizationInterval)
  {
    _visibleSectorListeners.add(new VisibleSectorListenerEntry(listener, stabilizationInterval));
  }

  /**
   Add a listener for notification of visible-sector changes.

   The event is triggered immediately without waiting for the visible-sector get settled.
   */
  public final void addVisibleSectorListener(VisibleSectorListener listener)
  {
    addVisibleSectorListener(listener, TimeInterval.zero());
  }

  /**
   * Set the download-priority used by Tiles (for downloading textures).
   *
   * @param texturePriority: new value for download priority of textures
   */
  public final void setTexturePriority(long texturePriority)
  {
    _texturePriority = texturePriority;
  }

  /**
   * Return the current value for the download priority of textures
   *
   * @return _texturePriority: long
   */
  public final long getTexturePriority()
  {
    return _texturePriority;
  }

  /**
   * @see Renderer#isPlanetRenderer()
   */
  public final boolean isPlanetRenderer()
  {
    return true;
  }

  public final SurfaceElevationProvider getSurfaceElevationProvider()
  {
    return (_elevationDataProvider == null) ? null : this;
  }

  public final PlanetRenderer getPlanetRenderer()
  {
    return this;
  }

  public final void addListener(Angle latitude, Angle longitude, SurfaceElevationListener listener)
  {
    _elevationListenersTree.add(new Geodetic2D(latitude, longitude), listener);
  }

  public final void addListener(Geodetic2D position, SurfaceElevationListener listener)
  {
    _elevationListenersTree.add(position, listener);
  }

  public final boolean removeListener(SurfaceElevationListener listener)
  {
    return _elevationListenersTree.remove(listener);
  }

  public final void sectorElevationChanged(ElevationData elevationData)
  {
    if (elevationData != null)
    {
      _elevationListenersTree.notifyListeners(elevationData, _verticalExaggeration);
    }
  }

  public final Sector getRenderedSector()
  {
    return _renderedSector;
  }

  public final void setRenderedSector(Sector sector)
  {
    if ((_renderedSector != null && !_renderedSector.isEquals(sector)) || (_renderedSector == null && !sector.isEquals(Sector.fullSphere())))
    {
      if (_renderedSector != null)
         _renderedSector.dispose();
  
      if (sector.isEquals(Sector.fullSphere()))
      {
        _renderedSector = null;
      }
      else
      {
        _renderedSector = new Sector(sector);
      }
    }
  
    _tessellator.setRenderedSector(sector);
  
    changed();
  }

  public final void addTerrainTouchListener(TerrainTouchListener listener)
  {
    if (listener != null)
    {
      _terrainTouchListeners.add(listener);
    }
  }

  public final void setElevationDataProvider(ElevationDataProvider elevationDataProvider, boolean owned)
  {
  
    if (_elevationDataProvider != elevationDataProvider)
    {
  
      if (_ownsElevationDataProvider)
      {
        if (_elevationDataProvider != null)
           _elevationDataProvider.dispose();
      }
  
      _ownsElevationDataProvider = owned;
      _elevationDataProvider = elevationDataProvider;
  
      if (_elevationDataProvider != null)
      {
        _elevationDataProvider.setChangedListener(this);
        if (_context != null)
        {
          _elevationDataProvider.initialize(_context); //Initializing EDP in case it wasn't
        }
      }
  
      changed();
    }
  }
  public final void setVerticalExaggeration(float verticalExaggeration)
  {
    if (_verticalExaggeration != verticalExaggeration)
    {
      _verticalExaggeration = verticalExaggeration;
      changed();
    }
  }

  public final ElevationDataProvider getElevationDataProvider()
  {
    return _elevationDataProvider;
  }

}