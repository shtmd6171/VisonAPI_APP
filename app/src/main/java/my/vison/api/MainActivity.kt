package my.vison.api

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.support.v4.content.FileProvider
import android.Manifest
import android.app.Activity
import android.graphics.Bitmap
import android.net.Uri
import android.widget.ImageView
import kotlinx.android.synthetic.*
import kotlinx.android.synthetic.main.activity_main.upload_image
import kotlinx.android.synthetic.main.main_analyze_view.*
import java.io.File

class MainActivity : AppCompatActivity() {

    private val CAMERA_PERMISSION_REQUEST = 1000
    private val GALLERY_PERMISSION_REQUEST = 1001
    private val FILE_NAME = "picture.jpg"
    private var uploadChooser : UploadChooser? = null
    private var labelDetectionTask : LabelDetectionTest? = null
    val LABEL_DETECTION_REQUEST = "label_detection_request"
    val LANDMARK_DETECTION_REQUEST = "landmark_detection_request"
    val TEXT_DETECITON_REQUEST = "Text_detection_request"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        labelDetectionTask = LabelDetectionTest(packageName,packageManager,this,header,uploaded_image_result)
        setupListener()
    }

    private fun setupListener() {
        upload_image.setOnClickListener {
            uploadChooser = UploadChooser().apply {
                // 매개변수인 interface 타입의 listener를 매개 변수 안에서
                // interface를 바로 불러와 implements 해준다
                addNotifier(object : UploadChooser.UploadChooserNotifierInterface{
                override fun cameraOnClick() {
                    checkCameraPermission()

                }

                override fun galleryOnClick() {
                    checkGalleryPermission()
                }
            })}
            uploadChooser!!.show(supportFragmentManager, "")
        }
    }

    // checkCameraPermission()은 권한이 있을 때만  openCamera()를 사용하기 때문에
    // 권한이 없는 상태로(최초로) 수행하게 되면 onRequestPermissionsResult()에서 동작한다
    private fun checkCameraPermission() {
        //requestPermission은 true를 return하기 때문에 카메라 권한을 얻으면 openCamera()를 실행한다
        if(PermissionUtil().requestPermission(this,CAMERA_PERMISSION_REQUEST, Manifest.permission.CAMERA,
                Manifest.permission.READ_EXTERNAL_STORAGE))
        {
            openCamera()
        }
    }
    private fun checkGalleryPermission(){
        if(PermissionUtil().requestPermission(this,GALLERY_PERMISSION_REQUEST, Manifest.permission.READ_EXTERNAL_STORAGE))
        {
            openGallery()
        }
    }

    private fun openCamera() {
        // Uri는 경로를 지칭함, photoUri는 카메라로 찍은 사진이 저장될 위치
        // 해당 Uri를 만들기 위해서는 FileProvider.getUriForFile()이라는 파일을 위한 Uri를 생성하는 함수를 사용
        // 매개변수는 Context, authority(authority는 해당 패키지명 + provider임), 파일이 필요함

        // FileProvider를 Manifest에 등록해줘야 사용 가능함
        val photoUri = FileProvider.getUriForFile(this,applicationContext.packageName + ".provider",createCameraFile())

        startActivityForResult(
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply{
            // 찍은 사진 (MediaStore.EXTRA_OUTPUT)을 photoUri에 넣어준다
            putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) },CAMERA_PERMISSION_REQUEST)
    }
    private fun openGallery(){
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply{
            setType("image/*")
            setAction(Intent.ACTION_GET_CONTENT)
        }
        startActivityForResult(Intent.createChooser(intent,"-"),GALLERY_PERMISSION_REQUEST)

    }

    // 사용자가 갤러리에서 사진을 선택할 때, 또는 사용자가 사진을 찍고난 후에 OK를 눌렀을 때
    // 이 이후의 동작이 여기서 동작한다
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when(requestCode) {
            // data에 사용자가 지정한 사진의 Uri를 지니고 있음
            GALLERY_PERMISSION_REQUEST ->  data?.let {uploadImage(it.data) }
            CAMERA_PERMISSION_REQUEST -> {
                // 작업의 결과물을 떠나 작업이 잘 수행되어 있는지 RESULT_OK로 확인해줘야 함
                // 작업이 잘 수행되지 않으면 바로 return함
                if(resultCode != Activity.RESULT_OK) return
                // openCamera()에서 찍은 결과물을 저장한 파일의 Uri를 다시 가져옴
                val photoUri = FileProvider.getUriForFile(this, applicationContext.packageName+".provider",createCameraFile())
                uploadImage(photoUri)

            }
        }
    }

    // 비트맵 객체를 통해 Uri를 Bitmap 형태로 얻는 함수
    private fun uploadImage(imageUri : Uri){
        // Bitmap 형태를 가져오기 위해서 contentResolver와 이미지의 Uri가 필요함
        // contentResolver는 Activity 자체에서 상속되어있는 메소드
        val bitmap : Bitmap = MediaStore.Images.Media.getBitmap(contentResolver, imageUri)

        // 이미지를 설정한 후에 uploadChooser를 닫게 해줌
        // uploadImage가 카메라,갤러리 어느 것을 선택해도 이것을 실행하기 때문에 여기에 적어뒀음
        uploadChooser?.dismiss()

        DetectionChooser().apply {
            addDetectionChooserNotifierInterface(object : DetectionChooser.DetectionChooserNotifierInterface {
                override fun detectLable() {
                    findViewById<ImageView>(R.id.uploaded_image).setImageBitmap(bitmap)
                    requestCloudVisionApi(bitmap,LABEL_DETECTION_REQUEST)
                }
                override fun detectLanmark() {
                    findViewById<ImageView>(R.id.uploaded_image).setImageBitmap(bitmap)
                    requestCloudVisionApi(bitmap,LANDMARK_DETECTION_REQUEST)
                }

                override fun detectText() {
                    findViewById<ImageView>(R.id.uploaded_image).setImageBitmap(bitmap)
                    requestCloudVisionApi(bitmap,TEXT_DETECITON_REQUEST)
                }

                override fun detectCancle() {
                    findViewById<ImageView>(R.id.uploaded_image).setImageBitmap(null)
                }
            })
        }.show(supportFragmentManager,"")
    }

    private fun requestCloudVisionApi(bitmap: Bitmap, requestType : String){
        labelDetectionTask?.requestCloudVisionApi(bitmap,requestType)
    }


    // 파일을 만드는 함수
    private fun createCameraFile() : File {

        // getExternalFilesDir()을 통해 디렉터리를 만드는데, 매개변수는 디렉터리로 삼을 저장소를 의미한다
        // 여기서 Environment.DIRECTORY_PICTURES란, 핸드폰 내부의 사진첩폴더를 디렉터리로 삼는 다는 것
        val dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        // 파일을 만들기 위해서는 디렉터리와 파일 이름이 필요함, 파일 네임(저장될 사진 이름)은 위에 지정했음
        return File(dir, FILE_NAME)
    }

    // Intent 값을 받아온 작업을 수행하는 Permission 형태의 함수
    // 일반 Intent의 onActivityResult처럼 Override 했음

    // openCamera()를 실행하는 checkCameraPermission()와의 차이점은
    // onRequestPermissionsResult()는 권한을 얻은 직후에 openCamera()를 사용하는 것
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>,grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode) {
            GALLERY_PERMISSION_REQUEST -> {
                if(PermissionUtil().permissionGranted(requestCode, GALLERY_PERMISSION_REQUEST, grantResults))
                {
                    openGallery()
                }
            }
            CAMERA_PERMISSION_REQUEST ->
            {
                // 여기서 requestCode는 startActivityForResult를 수행할 때 넘겨준 RequestCode이고
                // permissionGranted() 내부에서 RequestCode === CAMERA_PERMISSION_REQUEST인지 확인한다
                // grantResults는 권한을 얻은 IntArray형태로 저장한 것
                if(PermissionUtil().permissionGranted(requestCode, CAMERA_PERMISSION_REQUEST, grantResults))
                {
                    openCamera()
                }
            }
        }
    }
}


