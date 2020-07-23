package my.vison.api

import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.AsyncTask
import android.widget.TextView
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.vision.v1.Vision
import com.google.api.services.vision.v1.VisionRequest
import com.google.api.services.vision.v1.VisionRequestInitializer
import com.google.api.services.vision.v1.model.*
import java.io.ByteArrayOutputStream
import java.lang.Exception
import java.lang.StringBuilder
import java.lang.ref.WeakReference
import java.util.*
import kotlin.collections.ArrayList

class LabelDetectionTest(private val packageName : String, private val packageManager : PackageManager, private val activity: MainActivity,
                         private val header : TextView, private val uploaded: TextView) {


    private val CLOUD_VISION_KEY = "AIzaSyDZK7btlThyRgzL4WXoJujnnmgszDCGsHY"
    private val ANDROID_PACKAGE_HEADER = "X-Android-package"
    private val ANDROID_CERT_HEADER = "X-Android_Cert"
    private val MAX_LABEL_RESULT = 10
    private var requestType : String? = null



    fun requestCloudVisionApi(bitmap : Bitmap, requestType : String) {
        this.requestType = requestType
        val visionTask = ImageRequestTask(preapareImageRequest(bitmap),header,uploaded)
        visionTask.execute()

    }

    inner class ImageRequestTask constructor(val request : Vision.Images.Annotate,
                                             private val header : TextView,
                                             private val uploaded : TextView) : AsyncTask<Any, Void, String>() {

        //Instance사용 후, 메모리에 대한 정리를 생성한 Instance가 하지 않는다
        private val weakReference : WeakReference<MainActivity>

        init {
            weakReference = WeakReference(activity)
        }


        override fun doInBackground(vararg params: Any?): String {
            try {
                val response = request.execute()
                // Vision.Images.Annotate 형태의 응답을 String 타입으로 return 해준다
                // doInBackground()가 String타입이기 때문에 이에 맞게 설정해줌
                header.text = "분석 성공"
                return convertResponseToString(response)
            }catch (e : Exception){
                e.printStackTrace()
            }
            header.text = "분석 실패"
            return "분석 실패"
        }

        override fun onPostExecute(result: String?) {
            uploaded.text = result

        }
    }

    private fun preapareImageRequest(bitmap: Bitmap) : Vision.Images.Annotate {
        val httpTranport = AndroidHttp.newCompatibleTransport()
        val jsonFactory = GsonFactory.getDefaultInstance()

        // 요청시에 헤더를 지정함
        // 헤더에 KEY, VALUE를 실어 보냄
        val requestInitializer = object  : VisionRequestInitializer(CLOUD_VISION_KEY){
            override fun initializeVisionRequest(request: VisionRequest<*>?) {
                super.initializeVisionRequest(request)

                val packageName = packageName
                request?.requestHeaders?.set(ANDROID_PACKAGE_HEADER, packageName)
                val sig = PackageManagerUtil().getSignature(packageManager,packageName)
                request?.requestHeaders?.set(ANDROID_CERT_HEADER,sig)
            }
        }
        val builder = Vision.Builder(httpTranport, jsonFactory, null)
        builder.setVisionRequestInitializer(requestInitializer)
        val vision = builder.build()

        val batchAnnotateImagesRequest = BatchAnnotateImagesRequest()
        batchAnnotateImagesRequest.requests = object : ArrayList<AnnotateImageRequest>(){

            //사진첩이나 카메라로 가져온 이미지를 일정 비트맵 수준으로 지정후 바이트화 시킨 부분
            init {
                val annotateImageRequest = AnnotateImageRequest()
                val base64EncodedImage = Image()
                val byteArrayOutputStream = ByteArrayOutputStream()

                // 이미지의 퀄리티를 일정 범위로 지정하는 부분
                // 압축된 bitmap을 byteArrayOutputStream에 넣어준다
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream)

                // 이미지를 Byte형태로 변경해서 통신 가능한 형태로 바꿔준다
                val imageBytes = byteArrayOutputStream.toByteArray()

                base64EncodedImage.encodeContent(imageBytes)
                annotateImageRequest.image = base64EncodedImage

                // 보낼 요청의 설정, 이미지의 특징들을 labelDetection로 받고
                // 그 특징들의 총 갯수와 타입을 정함
                annotateImageRequest.features = object : ArrayList<Feature>(){
                    init {
                        val labelDetection = Feature()

                        when(requestType){
                            activity.LABEL_DETECTION_REQUEST ->
                            {
                                labelDetection.type = "LABEL_DETECTION"
                            }
                            activity.LANDMARK_DETECTION_REQUEST ->
                            {
                                labelDetection.type = "LANDMARK_DETECTION"
                            }
                            activity.TEXT_DETECITON_REQUEST ->
                            {
                                labelDetection.type = "TEXT_DETECTION"
                            }
                        }
                        labelDetection.maxResults = MAX_LABEL_RESULT
                        add(labelDetection)
                    }
                }
                add(annotateImageRequest)
            }
        }
        val annotateRequest = vision.images().annotate(batchAnnotateImagesRequest)
        annotateRequest.setDisableGZipContent(true)
        return annotateRequest
    }

    // API에 이미지를 전달하면 BatchAnnotateImagesResponse 라는 형태로 보내온다
    private fun convertResponseToString(response : BatchAnnotateImagesResponse) : String {
        val message = StringBuilder("분석 결과\n")
        var labels : List<EntityAnnotation>? = null
        when(requestType){
            activity.LABEL_DETECTION_REQUEST ->
            {
                labels = response.responses[0].labelAnnotations
            }
            activity.LANDMARK_DETECTION_REQUEST ->
            {
                labels = response.responses[0].landmarkAnnotations
            }
            activity.TEXT_DETECITON_REQUEST ->
            {
                labels = response.responses[0].textAnnotations
            }

        }
        // if (labels != null) { forEach (EntityAnnotation it : response) {...} }
        labels?.let { it.forEach {
            // StringBuilder.append를 통해 message에 있던 값 오른쪽에 append()의 내용을 추가함
            // 첫 번 째 매개변수에는 지역 타입을 설정하고
            // String.format을 통해 String타입의 지정타입을 결정함
            when (requestType) {
                activity.LABEL_DETECTION_REQUEST -> {
                    message.append(
                        String.format(
                            Locale.US,
                            "%.2f : %s ",
                            (it.score * 10),
                            it.description
                        )
                    )
                    message.append("\n")
                }
                activity.LANDMARK_DETECTION_REQUEST -> {
                    message.append(
                        String.format(
                            Locale.US,
                            "%.2f : %s ",
                            (it.score * 10),
                            it.description
                        )
                    )
                    message.append("\n")
                }
                activity.TEXT_DETECITON_REQUEST -> {
                    message.append(
                        String.format(
                            Locale.ENGLISH,
                            "%s ",
                            it.description
                        )
                    )
                }

            }
        }
           return message.toString()
        }
        return "분석실패"

    }

}