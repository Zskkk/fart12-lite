package cn.zskkk;

import android.app.ActivityThread;
import android.app.Application;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class FFaarrtt {
    //为了反射封装，根据类名和字段名，反射获取字段
    public static Field getClassField(ClassLoader classloader, String class_name,
                                      String filedName) {

        try {
            Class obj_class = classloader.loadClass(class_name);//Class.forName(class_name);
            Field field = obj_class.getDeclaredField(filedName);
            field.setAccessible(true);
            return field;
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;

    }

    public static Object getClassFieldObject(ClassLoader classloader, String class_name, Object obj,
                                             String filedName) {

        try {
            Class obj_class = classloader.loadClass(class_name);//Class.forName(class_name);
            Field field = obj_class.getDeclaredField(filedName);
            field.setAccessible(true);
            Object result = null;
            result = field.get(obj);
            return result;
            //field.setAccessible(true);
            //return field;
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;

    }

    public static Object invokeStaticMethod(String class_name,
                                            String method_name, Class[] pareTyple, Object[] pareVaules) {

        try {
            Class obj_class = Class.forName(class_name);
            Method method = obj_class.getMethod(method_name, pareTyple);
            return method.invoke(null, pareVaules);
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;

    }

    public static Object getFieldObject(String class_name, Object obj,
                                        String filedName) {
        try {
            Class obj_class = Class.forName(class_name);
            Field field = obj_class.getDeclaredField(filedName);
            field.setAccessible(true);
            return field.get(obj);
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        return null;

    }

    public static Application getCurrentApplication(){
        Object currentActivityThread = invokeStaticMethod(
                "android.app.ActivityThread", "currentActivityThread",
                new Class[]{}, new Object[]{});
        Object mBoundApplication = getFieldObject(
                "android.app.ActivityThread", currentActivityThread,
                "mBoundApplication");
        Application mInitialApplication = (Application) getFieldObject("android.app.ActivityThread",
                currentActivityThread, "mInitialApplication");
        Object loadedApkInfo = getFieldObject(
                "android.app.ActivityThread$AppBindData",
                mBoundApplication, "info");
        Application mApplication = (Application) getFieldObject("android.app.LoadedApk", loadedApkInfo, "mApplication");
        return mApplication;
    }

    public static ClassLoader getClassloader() {
        ClassLoader resultClassloader = null;
        Object currentActivityThread = invokeStaticMethod(
                "android.app.ActivityThread", "currentActivityThread",
                new Class[]{}, new Object[]{});
        Object mBoundApplication = getFieldObject(
                "android.app.ActivityThread", currentActivityThread,
                "mBoundApplication");
        Application mInitialApplication = (Application) getFieldObject("android.app.ActivityThread",
                currentActivityThread, "mInitialApplication");
        Object loadedApkInfo = getFieldObject(
                "android.app.ActivityThread$AppBindData",
                mBoundApplication, "info");
        Application mApplication = (Application) getFieldObject("android.app.LoadedApk", loadedApkInfo, "mApplication");
        Log.e("zskkk", "go into app->" + "packagename:" + mApplication.getPackageName());
        resultClassloader = mApplication.getClassLoader();
        return resultClassloader;
    }
    
    public static String getBlacklistClass(){
		String processName = ActivityThread.currentProcessName();
		BufferedReader br = null;
		String blackPath = "/data/local/tmp/"+processName+"_blacklist";
		StringBuilder sb = new StringBuilder();
		try{
			br = new BufferedReader(new FileReader(blackPath));
			String line;
			while ((line = br.readLine()) != null){
				sb.append(line + ",");
			}
			br.close();
		}
		catch (Exception ex){
			Log.e("zskkk", "getBlacklistClass err:"+ex.getMessage());
			return "";
		}
		return sb.toString();
    }

    public static boolean check_classname_in_blacklist(String classname,String blacklist){
        if(blacklist.equals("")){
			return false;
		}
		String[] list = blacklist.split(",");
		for (String s : list){
			if(!s.equals("") && classname.startsWith(s)){
				return true;
			}
		}
		return false;
    }


    //取指定类的所有构造函数，和所有函数，使用dumpMethodCode函数来把这些函数给保存出来
    public static void loadClassAndInvoke(ClassLoader appClassloader, String eachclassname, Method dumpMethodCode_method) {
        Class resultclass = null;
        Log.e("zskkk", "go into loadClassAndInvoke->" + "classname:" + eachclassname);
        try {
            resultclass = appClassloader.loadClass(eachclassname);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        } catch (Error e) {
            e.printStackTrace();
            return;
        }
        if (resultclass != null) {
            try {
                Constructor<?> cons[] = resultclass.getDeclaredConstructors();
                for (Constructor<?> constructor : cons) {
                    if (dumpMethodCode_method != null) {
                        try {
                            if(constructor.getName().contains("cn.zskkk.")){
                                continue;
                            }
                            Log.e("zskkk", "classname:" + eachclassname+ " constructor->invoke "+constructor.getName());
                            dumpMethodCode_method.invoke(null, constructor);
                        } catch (Exception e) {
                            e.printStackTrace();
                            continue;
                        } catch (Error e) {
                            e.printStackTrace();
                            continue;
                        }
                    } else {
                        Log.e("zskkk", "dumpMethodCode_method is null ");
                    }

                }
            } catch (Exception e) {
                e.printStackTrace();
            } catch (Error e) {
                e.printStackTrace();
            }
            try {
                Method[] methods = resultclass.getDeclaredMethods();
                if (methods != null) {
                    Log.e("zskkk", "classname:" + eachclassname+ " start invoke");
                    for (Method m : methods) {
                        if (dumpMethodCode_method != null) {
                            try {
                                if(m.getName().contains("cn.zskkk.")){
                                    continue;
                                }
                                Log.e("zskkk", "classname:" + eachclassname+ " method->invoke:" + m.getName());
                                dumpMethodCode_method.invoke(null, m);
                            } catch (Exception e) {
                                e.printStackTrace();
                                continue;
                            } catch (Error e) {
                                e.printStackTrace();
                                continue;
                            }
                        } else {
                            Log.e("zskkk", "dumpMethodCode_method is null ");
                        }
                    }
                    Log.e("zskkk", "go into loadClassAndInvoke->"   + "classname:" + eachclassname+ " end invoke");
                }
            } catch (Exception e) {
                e.printStackTrace();
            } catch (Error e) {
                e.printStackTrace();
            }
        }
    }

    //根据classLoader->pathList->dexElements拿到dexFile
    //然后拿到mCookie后，使用getClassNameList获取到所有类名。
    //loadClassAndInvoke处理所有类名导出所有函数
    //dumpMethodCode这个函数是fart自己加在DexFile中的
    public static void fartWithClassLoader(ClassLoader appClassloader) {
        Log.e("zskkk", "fartWithClassLoader "+appClassloader.toString());
        List<Object> dexFilesArray = new ArrayList<Object>();
        Field paist_Field = (Field) getClassField(appClassloader, "dalvik.system.BaseDexClassLoader", "pathList");
        Object pathList_object = getFieldObject("dalvik.system.BaseDexClassLoader", appClassloader, "pathList");
        Object[] ElementsArray = (Object[]) getFieldObject("dalvik.system.DexPathList", pathList_object, "dexElements");
        Field dexFile_fileField = null;
        try {
            dexFile_fileField = (Field) getClassField(appClassloader, "dalvik.system.DexPathList$Element", "dexFile");
        } catch (Exception e) {
            e.printStackTrace();
        } catch (Error e) {
            e.printStackTrace();
        }
        Class DexFileClazz = null;
        try {
            DexFileClazz = appClassloader.loadClass("dalvik.system.DexFile");
        } catch (Exception e) {
            e.printStackTrace();
        } catch (Error e) {
            e.printStackTrace();
        }
        Method getClassNameList_method = null;
        Method defineClass_method = null;
        Method dumpDexFile_method = null;
        Method dumpMethodCode_method = null;
        Method dumpRepair_method = null;

        for (Method field : DexFileClazz.getDeclaredMethods()) {
            if (field.getName().equals("getClassNameList")) {
                getClassNameList_method = field;
                getClassNameList_method.setAccessible(true);
            }
            if (field.getName().equals("defineClassNative")) {
                defineClass_method = field;
                defineClass_method.setAccessible(true);
            }
            if (field.getName().equals("dumpDexFile")) {
                dumpDexFile_method = field;
                dumpDexFile_method.setAccessible(true);
            }
            if (field.getName().equals("fartextMethodCode")) {
                dumpMethodCode_method = field;
                dumpMethodCode_method.setAccessible(true);
            }
            if (field.getName().equals("dumpRepair")) {
                dumpRepair_method = field;
                dumpRepair_method.setAccessible(true);
            }
        }
        Field mCookiefield = getClassField(appClassloader, "dalvik.system.DexFile", "mCookie");
        Log.e("zskkk->methods", "dalvik.system.DexPathList.ElementsArray.length:" + ElementsArray.length);
        for (int j = 0; j < ElementsArray.length; j++) {
            Object element = ElementsArray[j];
            Object dexfile = null;
            try {
                dexfile = (Object) dexFile_fileField.get(element);
            } catch (Exception e) {
                e.printStackTrace();
            } catch (Error e) {
                e.printStackTrace();
            }
            if (dexfile == null) {
                Log.e("zskkk", "dexfile is null");
                continue;
            }
            if (dexfile != null) {
                dexFilesArray.add(dexfile);
                Object mcookie = getClassFieldObject(appClassloader, "dalvik.system.DexFile", dexfile, "mCookie");
                if (mcookie == null) {
                    Object mInternalCookie = getClassFieldObject(appClassloader, "dalvik.system.DexFile", dexfile, "mInternalCookie");
                    if(mInternalCookie!=null)
                    {
                        mcookie=mInternalCookie;
                    }else{
                        Log.e("zskkk->err", "get mInternalCookie is null");
                        continue;
                    }

                }
                String[] classnames = null;
                try {
                    classnames = (String[]) getClassNameList_method.invoke(dexfile, mcookie);
                } catch (Exception e) {
                    e.printStackTrace();
                    continue;
                } catch (Error e) {
                    e.printStackTrace();
                    continue;
                }
                if (classnames != null) {
                    Log.e("zskkk", "all classes "+String.join(",",classnames));
					//add
					String blacklist = getBlacklistClass();
					//add end

                    for (String eachclassname : classnames) {
						//add
						if(check_classname_in_blacklist(eachclassname, blacklist)){
							Log.e("zskkk", "jump over classname->"+eachclassname);
							continue;
						}
						//add end
                        loadClassAndInvoke(appClassloader, eachclassname, dumpMethodCode_method);
                    }
                    if(dumpRepair_method != null){
                        Log.e("zskkk", "fartWithClassLoader dumpRepair");
                        try {
                            dumpRepair_method.invoke(null);
                        }catch(Exception ex){
                            Log.e("zskkk", "fartWithClassList dumpRepair invoke err:"+ex.getMessage());
                        }
                    }else{
                        Log.e("zskkk", "fartWithClassLoader dumpRepair is null");
                    }
                }

            }
        }
        return;
    }

    public static void fart() {
        Log.e("zskkk", "fart");
        ClassLoader appClassloader = getClassloader();
        if(appClassloader==null){
            Log.e("zskkk", "appClassloader is null");
            return;
        }
        ClassLoader tmpClassloader=appClassloader;
        ClassLoader parentClassloader=appClassloader.getParent();
        if(appClassloader.toString().indexOf("java.lang.BootClassLoader")==-1)
        {
            fartWithClassLoader(appClassloader);
        }
        while(parentClassloader!=null){
            if(parentClassloader.toString().indexOf("java.lang.BootClassLoader")==-1)
            {
                fartWithClassLoader(parentClassloader);
            }
            tmpClassloader=parentClassloader;
            parentClassloader=parentClassloader.getParent();
        }
    }

    public static PackageItem f1rtConfig;

    public static String getConfig(){
        BufferedReader br = null;
        String configPath="/data/local/tmp/f1rt.config";
        String result = "";
        try {
            br = new BufferedReader(new FileReader(configPath));
            String line;
            while ((line = br.readLine()) != null) {
                result += line + "\n";
            }
            br.close();
            return result;
        } catch (Exception ex) {
            Log.e("zskkk", "shouldUnpack err:"+ex.getMessage());
        }
        return "";
    }

    public static void initConfig() {
        try {
            String f1rtConfigJson = getConfig();
            Log.e("zskkk", "f1rt.config -> "+f1rtConfigJson);
            if (f1rtConfigJson == null || f1rtConfigJson.equals("")) {
                return;
            }

            JSONObject jobj = new JSONObject(f1rtConfigJson);
            f1rtConfig = new PackageItem();
            f1rtConfig.enabled=jobj.getBoolean("enabled");
            f1rtConfig.packageName = jobj.getString("packageName");
            f1rtConfig.appName = jobj.getString("appName");
            f1rtConfig.isTuoke = jobj.getBoolean("isTuoke");
            f1rtConfig.isDeep = jobj.getBoolean("isDeep");

        } catch(Exception ex) {
            Log.e("zskkk", "initConfig err:" + ex.getMessage());
            f1rtConfig = null;
            return;
        }
    }

    public static void SetRomConfig(PackageItem item){
        Log.e("zskkk", "SetRomConfig start");
        ClassLoader appClassloader = getClassloader();
        if(appClassloader==null){
            Log.e("zskkk", "SetRomConfig appClassloader is null");
            return;
        }
        Class DexFileClazz = null;
        try {
            DexFileClazz = appClassloader.loadClass("dalvik.system.DexFile");
        } catch (Exception e) {
            Log.e("zskkk", "SetRomConfig loadClass err:"+e.getMessage());
            e.printStackTrace();
        }
        Method setMikRomConfig_method = null;
        for (Method field : DexFileClazz.getDeclaredMethods()) {
            if (field.getName().equals("setMikRomConfig")) {
                setMikRomConfig_method = field;
                setMikRomConfig_method.setAccessible(true);
            }
        }
        if(setMikRomConfig_method==null){
            Log.e("zskkk", "SetRomConfig setMikRomConfig_method is null");
            return;
        }
        try{
            Log.e("zskkk", "SetRomConfig invoke");
            setMikRomConfig_method.invoke(null,item);
        }catch (Exception e) {
            Log.e("zskkk", "SetRomConfig setMikRomConfig_method.invoke "+e.getMessage());
            e.printStackTrace();
        }
    }

    public static PackageItem shouldUnpack() {
        String processName = ActivityThread.currentProcessName();
        if (f1rtConfig != null && f1rtConfig.packageName.equals(processName)) {
            if (f1rtConfig.isTuoke) {
                SetRomConfig(f1rtConfig);
                return f1rtConfig;
            }
        }
        Log.e("zskkk", "shouldUnpack null processName:"+processName);
        return null;
    }

    public static String getClassList() {
        String processName = ActivityThread.currentProcessName();
        BufferedReader br = null;
        String configPath="/data/local/tmp/"+processName;
        Log.e("zskkk", "getClassList processName:"+processName);
        StringBuilder sb=new StringBuilder();
        try {
            br = new BufferedReader(new FileReader(configPath));
            String line;
            while ((line = br.readLine()) != null) {

                if(line.length()>=2){
                    sb.append(line+"\n");
                }
            }
            br.close();
        }
        catch (Exception ex) {
            Log.e("zskkk", "getClassList err:"+ex.getMessage());
            return "";
        }
        return sb.toString();
    }

    public static void fartWithClassList(String classlist){
        ClassLoader appClassloader = getClassloader();
        if(appClassloader==null){
            Log.e("zskkk", "appClassloader is null");
            return;
        }
        Class DexFileClazz = null;
        try {
            DexFileClazz = appClassloader.loadClass("dalvik.system.DexFile");
        } catch (Exception e) {
            e.printStackTrace();
        } catch (Error e) {
            e.printStackTrace();
        }
        Method dumpMethodCode_method = null;
        for (Method field : DexFileClazz.getDeclaredMethods()) {
            if (field.getName().equals("fartextMethodCode")) {
                dumpMethodCode_method = field;
                dumpMethodCode_method.setAccessible(true);
            }
        }
        String[] classes=classlist.split("\n");
        for(String clsname : classes){
            String line=clsname;
            if(line.startsWith("L")&&line.endsWith(";")&&line.contains("/")){
                line=line.substring(1,line.length()-1);
                line=line.replace("/",".");
            }
            loadClassAndInvoke(appClassloader, line, dumpMethodCode_method);
        }
    }

    public static void fartthread() {
        PackageItem item = shouldUnpack();
        if (item == null || !item.isTuoke) {
            return;
        }
        String classlist=getClassList();
        if(!classlist.equals("")){
            fartWithClassList(classlist);
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                // TODO Auto-generated method stub
                try {
                    Log.e("zskkk", "start sleep......");
                    Thread.sleep(1 * 60 * 1000);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                Log.e("zskkk", "sleep over and start");
                fart();
                Log.e("zskkk", "run over");

            }
        }).start();
    }

}

