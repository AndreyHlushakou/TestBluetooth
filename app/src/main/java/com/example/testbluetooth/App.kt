package com.example.testbluetooth

import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import com.hjq.toast.Toaster

lateinit var mApplication: Application

class App : Application(){

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
//        if (ProcessPhoenix.isPhoenixProcess(this)) {
//            return
//        }
        mApplication = this
        Toaster.init(this)
//        LogUtils.init(BuildConfig.DEBUG)
//        Log.d("App","XCrash")
//        BRV.modelId = BR.item
//        XCrash.init(this, XCrash.InitParameters()
//            .setAnrCallback { logPath, emergency ->
//                Log.d("Anr",emergency)
//                resetApp()
//            }
//            .setJavaCallback { logPath, emergency ->
//                Log.d("Java",emergency)
//                resetApp()
//            }
//            .setNativeCallback { logPath, emergency ->
//                Log.d("Native",emergency)
//                resetApp()
//            }.setLogDir("${getExternalFilesDir(null)!!.absolutePath}/tombstones")
//        )
    }

//    private fun resetApp(){
//        ProcessPhoenix.triggerRebirth(this)
//    }

    override fun onCreate() {
        super.onCreate()
        Log.d("App","onCreate")
    }

}

object AppContext : ContextWrapper(mApplication)//ContextWrapper对Context上下文进行包装(装饰者模式)