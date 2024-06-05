package vn.kalapa.ekyc

interface DialogListener {
    fun onYes()
    fun onNo()
}

interface AlertListener {
    fun onYes()
}


interface KLPFaceOTPListener {
    fun complete(flowType: FaceOTPFlowType, userId: String, transactionId: String)
    fun cancel(flowType: FaceOTPFlowType, userId: String, message: String)

}
