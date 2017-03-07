package watsonservices.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.Calendar;

import org.apache.commons.io.IOUtils;
import org.joda.time.DateTime;

import com.mendix.core.Core;
import com.mendix.systemwideinterfaces.MendixException;
import com.mendix.systemwideinterfaces.core.IContext;
import com.mendix.systemwideinterfaces.core.IMendixObject;

import system.proxies.FileDocument;
import watsonservices.proxies.Speech;

public class WatsonClientUtils {
	
	static public Path getFilePath(IContext context, Speech speechObject) throws MendixException
    {

        Path mdaFileDestination = null;
        File tempFile = null;
        try
        {
        	String fileName = Long.toString(new java.util.Date().getTime());
        	tempFile = File.createTempFile(fileName, null);
         //   tempFile.deleteOnExit();

            try(final FileOutputStream out = new FileOutputStream(tempFile))
            {
                IOUtils.copy(Core.getFileDocumentContent(context, speechObject.getMendixObject()), out);
            }
        } catch (Exception e) {
            Core.getLogger("VPCManager").error("Copy the provided file " + speechObject.getName() + " failed to the destination " + mdaFileDestination, e);
            throw new MendixException(e);
        }
        return tempFile.toPath();
    }
	
}
