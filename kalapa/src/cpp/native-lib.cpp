#include <jni.h>
#include <string>
#include <asm/unistd.h>
# include <stdio.h>
# include <unistd.h>
#include <fcntl.h>

extern "C" JNIEXPORT jint
Java_vn_kalapa_ekyc_activity_BaseActivity_isPathReallyExist(JNIEnv *env, jobject, jstring path) {
    const char *cpath = env->GetStringUTFChars(path, nullptr);
    int result1 = syscall(__NR_faccessat,AT_FDCWD, cpath, F_OK, 0);
    return result1;
}