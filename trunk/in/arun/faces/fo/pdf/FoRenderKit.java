//
// Copyright 2014 Senthil Murugan Ramasamy
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0

// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//

package in.arun.faces.fo.pdf;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.faces.FactoryFinder;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseStream;
import javax.faces.context.ResponseWriter;
import javax.faces.context.ResponseWriterWrapper;
import javax.faces.render.RenderKit;
import javax.faces.render.RenderKitFactory;
import javax.faces.render.Renderer;
import javax.faces.render.ResponseStateManager;

public class FoRenderKit extends RenderKit {

    private static final String FALLBACK_RENDERKIT = "HTML_BASIC";

    static final Logger logger = Logger.getLogger("arun.pdf");

    @Override
    public void addRenderer(String family, String rendererType, Renderer renderer) {
    }

    @Override
    public Renderer getRenderer(String family, String rendererType) {
        FacesContext context = FacesContext.getCurrentInstance();
        RenderKitFactory factory = (RenderKitFactory) FactoryFinder.getFactory(FactoryFinder.RENDER_KIT_FACTORY);

        return factory.getRenderKit(context, FALLBACK_RENDERKIT).getRenderer(family, rendererType);
    }

    @Override
    public ResponseStateManager getResponseStateManager() {
        FacesContext context = FacesContext.getCurrentInstance();
        RenderKitFactory factory = (RenderKitFactory) FactoryFinder.getFactory(FactoryFinder.RENDER_KIT_FACTORY);

        return factory.getRenderKit(context, FALLBACK_RENDERKIT).getResponseStateManager();
    }
    
    @Override
    public ResponseStream createResponseStream(OutputStream stream) {
        return null;
    }

    @Override
    public ResponseWriter createResponseWriter(Writer origWriter, String contentTypeList, String characterEncoding) {
        FacesContext context = FacesContext.getCurrentInstance();
        RenderKitFactory factory = (RenderKitFactory) FactoryFinder.getFactory(FactoryFinder.RENDER_KIT_FACTORY);

        try {
            final ResponseWriter writer = factory.getRenderKit(context, FALLBACK_RENDERKIT)
                    .createResponseWriter(new OutputStreamWriter(new FoOutputStream()), contentTypeList, characterEncoding);

            return new ResponseWriterWrapper() {
                @Override
                public ResponseWriter cloneWithWriter(Writer writer) {
                    return this;
                }

                @Override
                public ResponseWriter getWrapped() {
                    return writer;
                }
            };

        } catch (IOException x) {
            logger.log(Level.WARNING, "Error while creating buffered output stream.", x);
            return null;
        }
    }
}
