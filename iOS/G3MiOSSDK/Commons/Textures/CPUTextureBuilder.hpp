//
//  CPUTextureBuilder.hpp
//  G3MiOSSDK
//
//  Created by José Miguel S N on 23/07/12.
//  Copyright (c) 2012 __MyCompanyName__. All rights reserved.
//

#ifndef G3MiOSSDK_CPUTextureBuilder_hpp
#define G3MiOSSDK_CPUTextureBuilder_hpp

#include "TextureBuilder.hpp"


class CPUTextureBuilder:public TextureBuilder
{
public:
  int createTextureFromImages(const RenderContext* rc, 
                              const std::vector<const IImage*>& vImages, 
                              int width, int height) const;
  
  int createTextureFromImages(const RenderContext* rc, 
                              const std::vector<const IImage*>& vImages, 
                              const std::vector<const Rectangle*>& vRectangles, 
                              int width, int height) const;
  
  void initialize(const InitializationContext* ic) {}
  
};

#endif
