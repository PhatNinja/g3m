//
//  G3MViewController.m
//  G3MApp
//
//  Created by Mari Luz Mateo on 18/02/13.
//  Copyright (c) 2013 Igo Software SL. All rights reserved.
//

#import "G3MViewController.h"
#import "G3MWebViewController.h"

#import <G3MiOSSDK/G3MWidget_iOS.h>
#import <G3MiOSSDK/G3MWidget.hpp>
#import <G3MiOSSDK/G3MBuilder_iOS.hpp>
#import <G3MiOSSDK/GInitializationTask.hpp>
#import <G3MiOSSDK/PeriodicalTask.hpp>
#import <G3MiOSSDK/IThreadUtils.hpp>
#import <G3MiOSSDK/TileRendererBuilder.hpp>
#import <G3MiOSSDK/LayerBuilder.hpp>
#import <G3MiOSSDK/Mark.hpp>
#import <G3MiOSSDK/MarkTouchListener.hpp>
#import <G3MiOSSDK/MarksRenderer.hpp>
#import <G3MiOSSDK/ShapesRenderer.hpp>
#import <G3MiOSSDK/MeshRenderer.hpp>
#import <G3MiOSSDK/Downloader_iOS.hpp>
#import <G3MiOSSDK/JSONBaseObject.hpp>
#import <G3MiOSSDK/IJSONParser.hpp>
#import <G3MiOSSDK/JSONObject.hpp>
#import <G3MiOSSDK/JSONArray.hpp>
#import <G3MiOSSDK/JSONNumber.hpp>
#import <G3MiOSSDK/JSONString.hpp>
#import <G3MiOSSDK/SceneJSShapesParser.hpp>
#import <G3MiOSSDK/FloatBufferBuilderFromGeodetic.hpp>
#import <G3MiOSSDK/FloatBufferBuilderFromColor.hpp>
#import <G3MiOSSDK/DirectMesh.hpp>
#import <G3MiOSSDK/ByteBuffer_iOS.hpp>
#import <G3MiOSSDK/BSONParser.hpp>
#import <G3MiOSSDK/JSONGenerator.hpp>
#import "G3MToolbar.h"

@interface G3MViewController ()

@end

@implementation G3MViewController

@synthesize demoSelector, demoMenu, toolbar, layerSwitcher, playButton;
@synthesize g3mWidget, layerSet, satelliteLayerEnabled, markerRenderer, shapeRenderer, meshRenderer;


- (id)initWithNibName:(NSString *)nibNameOrNil bundle:(NSBundle *)nibBundleOrNil
{
  self = [super initWithNibName:nibNameOrNil bundle:nibBundleOrNil];
  if (self) {
    // Custom initialization
  }
  return self;
}

- (void)viewDidLoad
{
  [super viewDidLoad];
  
  // Create a builder
  G3MBuilder_iOS builder([self g3mWidget]);
  
  [self setSatelliteLayerEnabled: true];
  [self setLayerSet: [self createLayerSet]];
  [self setMarkerRenderer: [self createMarkerRenderer]];
  [self markerRenderer]->setEnable(false);
  [self setShapeRenderer: new ShapesRenderer()];
  [self shapeRenderer]->setEnable(false);
  [self setMeshRenderer: [self createMeshRenderer: builder.getPlanet()]];
  [self meshRenderer]->setEnable(false);
  
  // Setup the builder
  builder.getTileRendererBuilder()->setLayerSet([self layerSet]);
  // store satellite layers names
  satelliteLayersNames = builder.getTileRendererBuilder()->getDefaultLayersNames();
  //  builder.getTileRendererBuilder()->setShowStatistics(true);
  builder.addRenderer([self markerRenderer]);
  builder.addRenderer([self shapeRenderer]);
  builder.addRenderer([self meshRenderer]);
  builder.setInitializationTask([self createInitializationTask], true);
  builder.setUserData(new DemoUserData());
  
  // Initialize widget
  builder.initializeWidget();
  [self resetWidget];
  
  [self initDropDownMenu];
  [self initToolbar];
}

- (void)viewDidAppear:(BOOL)animated
{
  [super viewDidAppear:animated];
  
  // Let's get the show on the road!
  [[self g3mWidget] startAnimation];
}

- (void)viewDidDisappear:(BOOL)animated
{
  // Stop the glob3 render
  [[self g3mWidget] stopAnimation];
  
	[super viewDidDisappear:animated];
}

- (void)viewDidUnload
{
  [self setG3mWidget: nil];
  [self setToolbar: nil];
  [self setLayerSwitcher: nil];
  [self setDemoSelector:nil];
  [self setDemoMenu: nil];
  [self setLayerSwitcher: nil];
  [self setPlayButton: nil];
  
  [self setMarkerRenderer: nil];
  [self setShapeRenderer: nil];
  [self setMeshRenderer: nil];
  [self setLayerSet: nil];
  
  [super viewDidUnload];
}

- (BOOL)shouldAutorotateToInterfaceOrientation:(UIInterfaceOrientation)interfaceOrientation
{
  // Return YES for supported orientations
  if ([[UIDevice currentDevice] userInterfaceIdiom] == UIUserInterfaceIdiomPhone) {
    return (interfaceOrientation != UIInterfaceOrientationPortraitUpsideDown);
  } else {
    return YES;
  }
}

- (void)didReceiveMemoryWarning
{
  [super didReceiveMemoryWarning];
  // Dispose of any resources that can be recreated.
}

- (LayerSet*) createLayerSet
{
  LayerSet* layers = LayerBuilder::createDefaultSatelliteImagery();
  
  layers->addLayer(LayerBuilder::createOSMLayer(![self satelliteLayerEnabled]));
  
  return layers;
}

- (MarksRenderer*) createMarkerRenderer
{  
  class TestMarkTouchListener : public MarkTouchListener {
  private:
    G3MViewController* _vc;
  public:
    TestMarkTouchListener(G3MViewController* vc) {
      _vc = vc;
    }
    bool touchedMark(Mark* mark) {
      NSString* message = [NSString stringWithFormat: @"%s", ((MarkerUserData*) mark->getUserData())->getTitle().c_str()];
      
      UIAlertView *alert = [[UIAlertView alloc] initWithTitle: @"glob3 mobile"
                                                      message: message
                                                     delegate: _vc
                                            cancelButtonTitle: @"OK"
                                            otherButtonTitles: @"Learn more...",nil];
      
      URL markUrl = ((MarkerUserData*) mark->getUserData())->getUrl();
      [_vc setValue: [NSString stringWithCString: markUrl.getPath().c_str()
                                        encoding: NSUTF8StringEncoding]
             forKey: @"urlMarkString"];
      [alert show];
      
      return true;
    }
  };
  
  const bool readyWhenMarksReady = false;
  MarksRenderer* marksRenderer = new MarksRenderer(readyWhenMarksReady);
  
  marksRenderer->setMarkTouchListener(new TestMarkTouchListener(self), true);
  
  return marksRenderer;
}

- (MeshRenderer*) createMeshRenderer: (const Planet*) planet
{
  MeshRenderer* mr = new MeshRenderer();
  
  FloatBufferBuilderFromGeodetic vertices(CenterStrategy::firstVertex(),
                                          planet,
                                          Geodetic2D::zero());
  FloatBufferBuilderFromColor colors;
  
  const Angle centerLat = Angle::fromDegreesMinutesSeconds(38, 53, 42);
  const Angle centerLon = Angle::fromDegreesMinutesSeconds(-77, 02, 11);
  
  const Angle deltaLat = Angle::fromDegrees(1).div(16);
  const Angle deltaLon = Angle::fromDegrees(1).div(16);
  
  const int steps = 128;
  const int halfSteps = steps/2;
  for (int i = -halfSteps; i < halfSteps; i++) {
    Angle lat = centerLat.add( deltaLat.times(i) );
    for (int j = -halfSteps; j < halfSteps; j++) {
      Angle lon = centerLon.add( deltaLon.times(j) );
      
      vertices.add( Geodetic3D(lat, lon, 100000) );
      
      const float red   = (float) (i + halfSteps + 1) / steps;
      const float green = (float) (j + halfSteps + 1) / steps;
      colors.add(Color::fromRGBA(red, green, 0, 1));
    }
  }
  
  const float lineWidth = 1;
  const float pointSize = 2;
  Color* flatColor = NULL;
  DirectMesh* pointMesh = new DirectMesh(GLPrimitive::points(),
                                         true,
                                         vertices.getCenter(),
                                         vertices.create(),
                                         lineWidth,
                                         pointSize,
                                         flatColor,
                                         colors.create());
  
  
  mr->addMesh(pointMesh);
  
  return mr;
}

- (GInitializationTask*) createInitializationTask
{
  class SampleInitializationTask : public GInitializationTask {
  private:
    bool _done;
    G3MWidget_iOS*  _widget;
    MarksRenderer* _marksRenderer;
    ShapesRenderer* _shapeRenderer;
    
  public:
    SampleInitializationTask(G3MWidget_iOS*  widget, MarksRenderer* mRenderer, ShapesRenderer* shapeRenderer) :
    _done(false),
    _widget(widget),
    _marksRenderer(mRenderer),
    _shapeRenderer(shapeRenderer)
    {
      
    }
    
    void run(const G3MContext* context) {
      // Markers
      Downloader_iOS* downloader = (Downloader_iOS*) context->getDownloader();
      
      downloader->requestBuffer(URL("http://poiproxy.mapps.es/browseByLonLat?service=wikilocation&lon=-122.415985&lat=37.766372&dist=50000", false),
                                200000,
                                TimeInterval::forever(),
                                new WikiMarkerBufferDownloadListener(this, _marksRenderer),
                                true);
      
      // 3D model
      context->getThreadUtils()->invokeInBackground(new PlaneParseTask(_widget, _shapeRenderer), true);
      
      _done = true;
    }
    
    bool isDone(const G3MContext* context) {
      return _done;
    }
  };
  
  GInitializationTask* initializationTask = new SampleInitializationTask([self g3mWidget],
                                                                         [self markerRenderer],
                                                                         [self shapeRenderer]);
  
  return initializationTask;
}

- (void) resetWidget
{
  [self markerRenderer]->setEnable(false);
  [self shapeRenderer]->setEnable(false);
  [self meshRenderer]->setEnable(false);
  
  [[self g3mWidget] widget]->stopCameraAnimation();
  [[self g3mWidget] widget]->resetCameraPosition();
  [[self g3mWidget] widget]->setCameraPosition(Geodetic3D(Angle::fromDegrees(0),
                                                          Angle::fromDegrees(0),
                                                          25000000));
  
  [[self g3mWidget] widget]->setAnimatedCameraPosition(Geodetic3D(Angle::fromDegrees(0),
                                                                  Angle::fromDegrees(0),
                                                                  25000000),
                                                       TimeInterval::fromSeconds(3));
}

- (void) showSimpleGlob3
{
  if (![self satelliteLayerEnabled]) {
    [self switchLayer];
  }
}

- (void) switchLayer
{
  [self setSatelliteLayerEnabled: ![self satelliteLayerEnabled]];
  
  if ([self satelliteLayerEnabled]) {
    [[self layerSwitcher] setImage:[UIImage imageNamed:@"satellite-on-96x48.png"] forState:UIControlStateNormal];
  }
  else {
    [[self layerSwitcher] setImage:[UIImage imageNamed:@"map-on-96x48.png"] forState:UIControlStateNormal];
  }
  
  // satellite layers
  for (int i = 0; i < satelliteLayersNames.size(); i++) {
    [self layerSet]->getLayer(satelliteLayersNames[i])->setEnable([self satelliteLayerEnabled]);
  }
  // osm
  [self layerSet]->getLayer("osm_auto:all")->setEnable(![self satelliteLayerEnabled]);
}

- (void) gotoMarkersDemo
{
  [[self g3mWidget] widget]->setAnimatedCameraPosition(Geodetic3D(Angle::fromDegrees(37.7658),
                                                                  Angle::fromDegrees(-122.4185),
                                                                  12000),
                                                       TimeInterval::fromSeconds(5));
  [[self toolbar] setVisible: FALSE];
}

- (void) gotoModelDemo
{
  [[self g3mWidget] widget]->setAnimatedCameraPosition(Geodetic3D(Angle::fromDegreesMinutesSeconds(38, 53, 42.24),
                                                                  Angle::fromDegreesMinutesSeconds(-77, 2, 10.92),
                                                                  6000),
                                                       TimeInterval::fromSeconds(5));
  
  Shape* plane = ((DemoUserData*) [[self g3mWidget] widget]->getUserData())->getPlane();
  plane->setAnimatedPosition(TimeInterval::fromSeconds(26),
                             Geodetic3D(Angle::fromDegreesMinutesSeconds(38, 53, 42.24),
                                        Angle::fromDegreesMinutesSeconds(-78, 2, 10.92),
                                        10000),
                             true);
  
  const double fromDistance = 50000 * 1.5;
  const double toDistance   = 25000 * 1.5 / 2;
  
  const Angle fromAzimuth = Angle::fromDegrees(-90);
  const Angle toAzimuth   = Angle::fromDegrees(-90 + 360);
  
  const Angle fromAltitude = Angle::fromDegrees(90);
  const Angle toAltitude   = Angle::fromDegrees(15);
  
  plane->orbitCamera(TimeInterval::fromSeconds(20),
                     fromDistance, toDistance,
                     fromAzimuth,  toAzimuth,
                     fromAltitude, toAltitude);
  
  [[self toolbar] setVisible: FALSE];
}

- (void) gotoMeshDemo
{
  [[self g3mWidget] widget]->setAnimatedCameraPosition(Geodetic3D(Angle::fromDegreesMinutesSeconds(38, 53, 42),
                                                                  Angle::fromDegreesMinutesSeconds(-77, 2, 11),
                                                                  6700000),
                                                       TimeInterval::fromSeconds(5));
  [[self toolbar] setVisible: FALSE];
}


// UI
- (void)alertView:(UIAlertView *)alertView clickedButtonAtIndex:(NSInteger)buttonIndex {
  NSString *title = [alertView buttonTitleAtIndex:buttonIndex];
  
  if([title isEqualToString:@"Learn more..."]) {
    G3MWebViewController *webView = [self.storyboard instantiateViewControllerWithIdentifier:@"G3MWebViewController"];
    [self presentModalViewController: webView
                            animated: YES];
    [webView loadUrl: [NSURL URLWithString:urlMarkString]];
  }
}

- (void) initDropDownMenu
{
  // demoSelector: left align button text and style
  [[self demoSelector] setContentHorizontalAlignment: UIControlContentHorizontalAlignmentLeft];
  [[self demoSelector] setContentEdgeInsets: UIEdgeInsetsMake(0, 10, 0, 0)];
  UIImage *demoSelectorBg = [UIImage imageNamed: @"selector-background.png"];
  [[self demoSelector] setBackgroundImage: demoSelectorBg forState: UIControlStateNormal];
  [[self demoSelector] setBackgroundImage: demoSelectorBg forState: UIControlStateHighlighted];
  
  [self setDemoMenu: [[G3MUIDropDownMenu alloc] initWithIdentifier: @"demoMenu"]];
  
  NSMutableArray *demoNames = [[NSMutableArray alloc] initWithObjects:
                               @"Simple glob3",
                               @"Switch layer",
                               @"Markers",
                               @"3D Model",
                               @"Point Mesh",
                               nil];
  
  [[self demoMenu] setDelegate: self];
  [[self demoMenu] setMenuWidth: 200];
  [[self demoMenu] setBackgroundColor: [UIColor darkGrayColor]];
  [[self demoMenu] setBorderColor: [UIColor blackColor]];
  [[self demoMenu] setTextColor: [UIColor lightGrayColor]];
  [[self demoMenu] setTitleArray: demoNames];
  [[self demoMenu] setValueArray: demoNames];
  [[self demoMenu] makeMenu: [self demoSelector]
                 targetView: [self view]];
}

- (UIButton*) createToolbarButton: (NSString*) imageName
                            frame: (CGRect) frame
{
  UIButton* button = [UIButton buttonWithType: UIButtonTypeCustom];
  [button setTitle: @""
          forState: nil];
  [[button layer] setBorderWidth: 0];
  [button setImage: [UIImage imageNamed: imageName]
          forState: UIControlStateNormal];
  [button setFrame: frame];
  
  return button;
}

- (void) initToolbar
{
  [self setToolbar: [[G3MToolbar alloc] init]];
  [[self view] addSubview: toolbar];
  
  // layerSwitcher
  [self setLayerSwitcher: [self createToolbarButton: @"satellite-on-96x48.png"
                                              frame: CGRectMake(10.0, 10.0, 96.0, 48.0)]];
  [[self layerSwitcher] addTarget: self
                           action: @selector(switchLayer)
                 forControlEvents: UIControlEventTouchUpInside];
  
  // playButton
  [self setPlayButton: [self createToolbarButton: @"play.png"
                                           frame: CGRectMake(10.0, 10.0, 48.0, 48.0)]];
}

- (void) updateToolbar: (NSString*) option
{
  [[self toolbar] clear];
  if ([option isEqual: @"Simple glob3"]) {
    [[self toolbar] setVisible: FALSE];
  }
  else if ([option isEqual: @"Switch layer"]) {
    [[self toolbar] addSubview: [self layerSwitcher]];
    [[self toolbar] setVisible: TRUE];
  }
  else if ([option isEqual: @"Markers"]) {
    [[self toolbar] addSubview: [self playButton]];
    [[self playButton] addTarget: self
                          action: @selector(gotoMarkersDemo)
                forControlEvents: UIControlEventTouchUpInside];
    [[self toolbar] setVisible: TRUE];
  }
  else if ([option isEqual: @"3D Model"]) {
    [[self toolbar] addSubview: [self playButton]];
    [[self playButton] addTarget: self
                          action: @selector(gotoModelDemo)
                forControlEvents: UIControlEventTouchUpInside];
    [[self toolbar] setVisible: TRUE];
  }
  else if ([option isEqual: @"Point Mesh"]) {
    [[self toolbar] addSubview: [self playButton]];
    [[self playButton] addTarget: self
                          action: @selector(gotoMeshDemo)
                forControlEvents: UIControlEventTouchUpInside];
    [[self toolbar] setVisible: TRUE];
  }
}

- (void) DropDownMenuDidChange: (NSString *) identifier
                              : (NSString *) returnValue
{
  if ([identifier isEqual: @"demoMenu"]) {
    [self resetWidget];
    [self updateToolbar: returnValue];
    [[self demoSelector] setTitle: returnValue
                         forState: nil];
    
    if ([returnValue isEqual: @"Simple glob3"]) {
      [self showSimpleGlob3];
    }
    else if ([returnValue isEqual: @"Switch layer"]) {
      [self switchLayer];
    }
    else if ([returnValue isEqual: @"Markers"]) {
      [self markerRenderer]->setEnable(true);
    }
    else if ([returnValue isEqual: @"3D Model"]) {
      [self shapeRenderer]->setEnable(true);
    }
    else if ([returnValue isEqual: @"Point Mesh"]) {
      [self meshRenderer]->setEnable(true);
    }
  }
}

// end UI

// Aux classes

class DemoUserData : public WidgetUserData {

private:
  Shape* _plane;

public:
  DemoUserData() {
    
  }
  
  void setPlane(Shape* plane) {
    _plane = plane;
  }
  
  Shape* getPlane() {
    return _plane;
  }
};



class MarkerUserData : public MarkUserData {
  
private:
  const std::string _title;
  const URL _url;
  
public:
  MarkerUserData(std::string title, const URL &url) :
  _title(title),
  _url(url) {
  }
  
  std::string getTitle() {
    return _title;
  }
  
  URL getUrl() {
    return _url;
  }
  
};


class WikiMarkerBufferDownloadListener : public IBufferDownloadListener {
  
private:
  GInitializationTask* _initTask;
  MarksRenderer* _markRenderer;
  
public:
  WikiMarkerBufferDownloadListener(GInitializationTask* initTask, MarksRenderer* markRenderer) {
    _initTask = initTask;
    _markRenderer = markRenderer;
  }
  void onDownload(const URL& url,
                  IByteBuffer* buffer) {
    JSONBaseObject* json = IJSONParser::instance()->parse(buffer->getAsString());
    const JSONArray* features = json->asObject()->getAsArray("features");
    
    for (int i = 0; i < features->size(); i++) {
      const JSONObject* item = features->getAsObject(i);
      
      const JSONObject* properties = item->asObject()->getAsObject("properties");
      std::string title = properties->getAsString("title")->value();
      std::string urlStr = properties->getAsString("url")->value();
      
      const JSONArray* coordinates = item->asObject()->getAsObject("geometry")->asObject()->getAsArray("coordinates");
      
      std::string markerIcon = "";
      if ([[UIScreen mainScreen] bounds].size.width < 768) {
        markerIcon = "file:///marker-wikipedia-48x48.png"; // iPhone
      }
      else {
        markerIcon = "file:///marker-wikipedia-72x72.png"; // iPad
      }
      
      Mark* marker = new Mark(URL(markerIcon, false),
                              Geodetic3D(Angle::fromDegrees(coordinates->getAsNumber(1)->value()),
                                         Angle::fromDegrees(coordinates->getAsNumber(0)->value()),
                                         0));
      
      MarkUserData* mud = new MarkerUserData(title, URL(urlStr, false));
      marker->setUserData(mud);
      
      _markRenderer->addMark(marker);
    }
    delete buffer;
    IJSONParser::instance()->deleteJSONData(json);
  }
  
  void onError(const URL& url) {
    NSString* message = [NSString stringWithFormat: @"Oops!\nThere was a problem getting markers info"];
    
    UIAlertView *alert = [[UIAlertView alloc] initWithTitle: @"glob3 mobile"
                                                    message: message
                                                   delegate: nil
                                          cancelButtonTitle: @"OK"
                                          otherButtonTitles: nil];
    [alert show];
  }
  
  void onCancel(const URL& url) {
    
  }
  
  void onCanceledDownload(const URL& url,
                          IByteBuffer* data) {
    
  }
  
};


class AddPlaneTask : public GTask {

private:
  ShapesRenderer* _shapeRenderer;
  Shape* _plane;

public:
  AddPlaneTask(ShapesRenderer* shapeRenderer, Shape* plane) {
    _shapeRenderer = shapeRenderer;
    _plane = plane;
  }
  
  void run(const G3MContext* context) {
    _shapeRenderer->addShape(_plane);
  }
};


class PlaneParseTask : public GTask {

private:
  G3MWidget_iOS* _widget;
  ShapesRenderer* _shapeRenderer;

public:
  PlaneParseTask(G3MWidget_iOS* widget, ShapesRenderer* shapeRenderer){
    _widget = widget;
    _shapeRenderer = shapeRenderer;
  }
  
  void run(const G3MContext* context) {
    NSString *bsonFilePath = [[NSBundle mainBundle] pathForResource: @"A320"
                                                             ofType: @"bson"];
    if (bsonFilePath) {
      NSData* data = [NSData dataWithContentsOfFile: bsonFilePath];
      const int length = [data length];
      unsigned char* bytes = new unsigned char[ length ]; // will be deleted by IByteBuffer's destructor
      [data getBytes: bytes
              length: length];

      IByteBuffer* buffer = new ByteBuffer_iOS(bytes, length);
      if (buffer) {
        Shape* plane = SceneJSShapesParser::parseFromBSON(buffer, "file:///");
        if (plane) {
          ((DemoUserData*) [_widget widget]->getUserData())->setPlane(plane);
          plane->setPosition(new Geodetic3D(Angle::fromDegreesMinutesSeconds(38, 53, 42.24),
                                            Angle::fromDegreesMinutesSeconds(-77, 2, 10.92),
                                            10000) );
          const double scale = 200;
          plane->setScale(scale, scale, scale);
          plane->setPitch(Angle::fromDegrees(90));

          context->getThreadUtils()->invokeInRendererThread(new AddPlaneTask(_shapeRenderer, plane), true);
        }
        delete buffer;
      }
    }
    
    /*
    NSString *jsonFilePath = [[NSBundle mainBundle] pathForResource: @"A320"
                                                             ofType: @"json"];
    if (jsonFilePath) {
      NSString *nsPlaneJSON = [NSString stringWithContentsOfFile: jsonFilePath
                                                        encoding: NSUTF8StringEncoding
                                                           error: nil];
      if (nsPlaneJSON) {
        std::string planeJSON = [nsPlaneJSON UTF8String];
        Shape* plane = SceneJSShapesParser::parseFromJSON(planeJSON, "file:///");
        if (plane) {
          ((DemoUserData*) [_widget widget]->getUserData())->setPlane(plane);
          plane->setPosition(new Geodetic3D(Angle::fromDegreesMinutesSeconds(38, 53, 42.24),
                                            Angle::fromDegreesMinutesSeconds(-77, 2, 10.92),
                                            10000) );
          const double scale = 200;
          plane->setScale(scale, scale, scale);
          plane->setPitch(Angle::fromDegrees(90));
          
          context->getThreadUtils()->invokeInRendererThread(new AddPlaneTask(_shapeRenderer, plane), true);
        }
      }
    }
    */
  }
};


@end

