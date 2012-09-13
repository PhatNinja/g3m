//
//  Image_iOS.h
//  G3MiOSSDK
//
//  Created by José Miguel S N on 13/06/12.
//  Copyright (c) 2012 __MyCompanyName__. All rights reserved.
//


#ifndef G3MiOSSDK_Image_iOS_h
#define G3MiOSSDK_Image_iOS_h

#import <UIKit/UIKit.h>

#include <string>

#include "IImage.hpp"

class Image_iOS: public IImage {
private:
  UIImage* _image;
  NSData*  _sourceBuffer;
  
  Image_iOS(const Image_iOS& that);
  
  void operator=(const Image_iOS& that);
  
public:
  
  virtual ~Image_iOS() {
//    printf("break (point) on me!\n");
  }
  
  Image_iOS(UIImage* image,
            NSData* sourceBuffer) :
  _image(image),
  _sourceBuffer(sourceBuffer)
  {
    
  }
  
  Image_iOS(int width, int height);
  
  UIImage* getUIImage() const {
    return _image;
  }
  
  NSData* getSourceBuffer() const {
    return _sourceBuffer;
  }
  
  int getWidth() const {
    return (_image == NULL) ? 0 : (int) _image.size.width;
  }
  
  int getHeight() const {
    return (_image == NULL) ? 0 : (int) _image.size.height;
  }
  
  IImage* combineWith(const IImage& other,
                      int width, int height) const;
  
  IImage* combineWith(const IImage& other,
                      const Rectangle& rect,
                      int width, int height) const;
  
  IImage* subImage(const Rectangle& rect) const;
  
  IByteBuffer* createByteBufferRGBA8888(int width, int height) const;
  
  const std::string description() const;
  
  IImage* copy() const;
  
};

#endif
