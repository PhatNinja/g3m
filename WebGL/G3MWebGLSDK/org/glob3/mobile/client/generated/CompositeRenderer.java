package org.glob3.mobile.client.generated; 
//
//  CompositeRenderer.cpp
//  G3MiOSSDK
//
//  Created by José Miguel S N on 31/05/12.
//  Copyright (c) 2012 __MyCompanyName__. All rights reserved.
//

//
//  CompositeRenderer.h
//  G3MiOSSDK
//
//  Created by José Miguel S N on 31/05/12.
//  Copyright (c) 2012 __MyCompanyName__. All rights reserved.
//




public class CompositeRenderer extends Renderer
{
  private java.util.ArrayList<Renderer> _renderers = new java.util.ArrayList<Renderer>();

  private InitializationContext _ic; // CHANGED BY CONVERSOR RULE

  public CompositeRenderer()
  {
	  _ic = null;
	_renderers = new java.util.ArrayList<Renderer>();
  }

  public void dispose()
  {
  }

  public final void initialize(InitializationContext ic)
  {
	_ic = ic;
  
	for (int i = 0; i < _renderers.size(); i++)
	{
	  _renderers.get(i).initialize(ic);
	}
  }

  public final int render(RenderContext rc)
  {
	//rc->getLogger()->logInfo("CompositeRenderer::render()");
  
	int min = DefineConstants.MAX_TIME_TO_RENDER;
	for (int i = 0; i < _renderers.size(); i++)
	{
	  int x = _renderers.get(i).render(rc);
	  if (x < min)
		  min = x;
	}
	return min;
  }

  public final boolean onTouchEvent(TouchEvent touchEvent)
  {
	// the events are processed bottom to top
	for (int i = _renderers.size() - 1; i >= 0; i--)
	{
	  if (_renderers.get(i).onTouchEvent(touchEvent))
	  {
		return true;
	  }
	}
	return false;
  }

  public final void addRenderer(Renderer renderer)
  {
	_renderers.add(renderer);
	if (_ic != null)
	{
	  renderer.initialize(_ic);
	}
  }

  public final void onResizeViewportEvent(int width, int height)
  {
	// the events are processed bottom to top
	for (int i = _renderers.size() - 1; i >= 0; i--)
	{
	  _renderers.get(i).onResizeViewportEvent(width, height);
	}
  }
}