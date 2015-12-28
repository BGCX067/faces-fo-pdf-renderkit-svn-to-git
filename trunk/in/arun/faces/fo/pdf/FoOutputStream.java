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

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamSource;
import org.apache.commons.io.FilenameUtils;
import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.apps.MimeConstants;

public class FoOutputStream extends ResponseStream {

    static final Logger logger = Logger.getLogger("arun.pdf");

    private ByteArrayOutputStream output = null;
    private boolean closed = false;

    public byte[] getBytes() {
        return output.toByteArray();
    }

    public FoOutputStream() throws IOException {
        super();

        closed = false;
        output = new ByteArrayOutputStream();
    }

    @Override
    public void close() throws IOException {
        if (closed) {
            throw new IOException("Stream has already been closed");
        }

        closed = true;
        PdfResult result = buildPdf(this);

        FacesContext context = FacesContext.getCurrentInstance();

        HttpServletResponse response = (HttpServletResponse) context.getExternalContext().getResponse();
        HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();

        response.reset();

        String fileName;
        if (null == (fileName = request.getParameter("output"))) {
            fileName = FilenameUtils.getBaseName(request.getRequestURL().toString());
        }

        fileName += ".pdf";

        response.setContentType(result.contentType);
        response.setContentLength(result.content.length);
        response.setHeader("content-disposition", "inline; FileName=\"" + fileName + "\"");

        OutputStream outputStream = response.getOutputStream();
        outputStream.write(result.content);
        outputStream.flush();

        context.responseComplete();
    }

    @Override
    public void flush() throws IOException {
        if (closed) {
            throw new IOException("Stream has already been closed");
        }

        output.flush();
    }

    @Override
    public void write(int b) throws IOException {
        if (closed) {
            throw new IOException("Cannot write to a closed output stream");
        }

        output.write((byte) b);
    }

    @Override
    public void write(byte[] b) throws IOException {
        write(b, 0, b.length);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        if (closed) {
            throw new IOException("Cannot write to a closed output stream");
        }

        output.write(b, off, len);
    }

    public void reset() {
    }

    private PdfResult buildPdf(FoOutputStream foOutput) {
        byte[] responseText = foOutput.getBytes();

        if (responseText == null) {
            return null;
        }

        PdfResult result = new PdfResult();

        try {
            PushbackInputStream pbis = new PushbackInputStream(new BufferedInputStream(new ByteArrayInputStream(responseText)));
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            //Skip contentType text/html - Looking for bug fix!
            //pbis.skip(9);
            while (pbis.available() > 0) {
                pbis.mark(1);
                if(pbis.read()=='<'){
                    pbis.unread('<');
                    break;
                }
            }

            //Transforming XML to PDF
            FopFactory fopFactory = FopFactory.newInstance();
            Fop fop = fopFactory.newFop(MimeConstants.MIME_PDF, baos);

            TransformerFactory tFactory = TransformerFactory.newInstance();
            Transformer transformer = tFactory.newTransformer();
            
            Source src = new StreamSource(pbis);
            Result res = new SAXResult(fop.getDefaultHandler());
            
            transformer.transform(src, res);

            result.contentType = MimeConstants.MIME_PDF;
            result.content = baos.toByteArray();
        } catch (IOException | FOPException | TransformerException x) {
            logger.log(Level.SEVERE, "Error while trying to create PDF.", x);

            StringBuilder builder = new StringBuilder();

            builder.append(x.getMessage());
            builder.append("<br/>");

            builder.append("<pre>");
            String formattedFo = new String(responseText);
            formattedFo = formattedFo.replaceAll("<", "&lt;");
            formattedFo = formattedFo.replaceAll(">", "&gt;");
            builder.append(formattedFo);
            builder.append("</pre>");

            result.contentType = "text/html";
            result.content = builder.toString().getBytes();
        }

        return result;
    }

    protected class PdfResult {

        protected PdfResult() {
        }

        protected PdfResult(String contentType, byte[] content) {
            this.contentType = contentType;
            this.content = content;
        }

        protected String contentType;
        protected byte[] content;
    }
}
