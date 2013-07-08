//
//  SGGeometryNode.hpp
//  G3MiOSSDK
//
//  Created by Diego Gomez Deck on 11/8/12.
//
//

#ifndef __G3MiOSSDK__SGGeometryNode__
#define __G3MiOSSDK__SGGeometryNode__

#include "SGNode.hpp"

#include "GLState.hpp"

class IFloatBuffer;
class IShortBuffer;
class GPUProgramState;

class SGGeometryNode : public SGNode {
private:
  const int     _primitive;
  IFloatBuffer* _vertices;
  IFloatBuffer* _colors;
  IFloatBuffer* _uv;
  IFloatBuffer* _normals;
  IShortBuffer* _indices;
  
  GLState _glState;
  void createGLState() const;

public:

  SGGeometryNode(const std::string& id,
                 const std::string& sId,
                 int                primitive,
                 IFloatBuffer*      vertices,
                 IFloatBuffer*      colors,
                 IFloatBuffer*      uv,
                 IFloatBuffer*      normals,
                 IShortBuffer*      indices) :
  SGNode(id, sId),
  _primitive(primitive),
  _vertices(vertices),
  _colors(colors),
  _uv(uv),
  _normals(normals),
  _indices(indices)
  {
    createGLState();
  }

  ~SGGeometryNode();


  void rawRender(const G3MRenderContext* rc,
                 const GLGlobalState& parentState, const GPUProgramState* parentProgramState);

  void rawRender(const G3MRenderContext* rc, GLState* glState);
  
  GLState* getGLState(GLState* parentGLState){
    _glState.setParent(parentGLState);
    return &_glState;
  }

  std::string description(){
    return "SGGeometryNode";
  }
  
};

#endif
