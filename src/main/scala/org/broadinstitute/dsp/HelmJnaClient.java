package org.broadinstitute.dsp;

import com.sun.jna.Library;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;

import java.util.Arrays;
import java.util.List;


public interface HelmJnaClient extends Library {
  // GoSlice class maps to:
  // C type struct { void *data; GoInt len; GoInt cap; }
  public class GoSlice extends Structure {
    public static class ByValue extends GoSlice implements Structure.ByValue {}
    public Pointer data;
    public long len;
    public long cap;
    protected List getFieldOrder(){
      return Arrays.asList(new String[]{"data","len","cap"});
    }
  }

  // GoString class maps to:
  // C type struct { const char *p; GoInt n; }
  public class GoString extends Structure {
    public static class ByValue extends GoString implements Structure.ByValue {}
    public String p;
    public long n;
    protected List getFieldOrder(){
      return Arrays.asList(new String[]{"p","n"});
    }

  }

  // Foreign functions
  public String installChart(
             GoString.ByValue namespace,
             GoString.ByValue kubeToken,
             GoString.ByValue kubeApiServer,
             GoString.ByValue caCertFile,
             GoString.ByValue release,
             GoString.ByValue chartName,
             GoString.ByValue chartVersion,
             GoString.ByValue values,
             byte createNamespace
           );

  public String uninstallRelease(
          GoString.ByValue namespace,
          GoString.ByValue kubeToken,
          GoString.ByValue kubeApiServer,
          GoString.ByValue caCertFile,
          GoString.ByValue release,
          byte keepHistory
  );

  public void listHelm(GoString.ByValue namespace,
                       GoString.ByValue kubeToken,
                       GoString.ByValue kubeApiServer,
                       GoString.ByValue caCertFile
                       );
}
