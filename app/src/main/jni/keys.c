#include <jni.h>

JNIEXPORT jstring JNICALL
Java_com_example_rfidstockpro_ui_activities_getKey(JNIEnv *env, jobject instance) {

    return (*env)-> NewStringUTF(env, "TmF0aXZlNWVjcmV0UEBzc3cwcmQx");
}