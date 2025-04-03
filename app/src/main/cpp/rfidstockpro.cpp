#include <string>
#include <jni.h>
#include <jni.h>

extern "C"
JNIEXPORT jstring JNICALL
Java_com_example_rfidstockpro_RFIDApplication_00024Companion_getAwsAccessKeyFromNdk(JNIEnv *env,
                                                                                    jobject thiz) {
    std::string apiKey = "AKIAU5LH6AA6PZMWLVGH";
    return env->NewStringUTF(apiKey.c_str());
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_example_rfidstockpro_RFIDApplication_00024Companion_getAwsSecretKeyFromNdk(JNIEnv *env,
                                                                                    jobject thiz) {
    std::string apiKey = "82uAgthAYF8t4Di5CNzJHtfS46BhKjnGhz9uWv7D";
    return env->NewStringUTF(apiKey.c_str());
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_example_rfidstockpro_RFIDApplication_00024Companion_getEncryptionKey(JNIEnv *env,
                                                                              jobject thiz) {
    std::string aesKey = "45698235674125896325412563698745";  // 🔒 Store your AES key here
    return env->NewStringUTF(aesKey.c_str());
}