package vn.kalapa.ekyc

interface DialogListener {
    fun onYes()
    fun onNo()
}

interface AlertListener {
    fun onYes()
}


interface KLPFaceOTPListener {
    fun complete(flowType: KalapaFlowType, userId: String, transactionId: String)
    fun cancel(flowType: KalapaFlowType, userId: String, message: String)

}
