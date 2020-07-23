package my.vison.api

import android.os.Bundle
import android.support.design.widget.BottomSheetDialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.upload_chooser.*

class UploadChooser : BottomSheetDialogFragment() {


    interface UploadChooserNotifierInterface {
        fun cameraOnClick()
        fun galleryOnClick()
    }

    var uploadChooserNotifierInterface : UploadChooserNotifierInterface? = null

    // addNotifier는 MainActivity에서 UploadChooserNotifierInterface의
    // interface를 사용하게 하는 함수로 MainActivity 내 에서는 익명 객체로 선언했다
    fun addNotifier(listener : UploadChooserNotifierInterface) {
        uploadChooserNotifierInterface = listener
    }


    override fun onCreateView(inflater: LayoutInflater,container: ViewGroup?,savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.upload_chooser,container,false)

    }
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setupListener()
    }

    private fun setupListener(){
        upload_camera.setOnClickListener {
            uploadChooserNotifierInterface?.cameraOnClick()
        }
        upload_gallery.setOnClickListener {
            uploadChooserNotifierInterface?.galleryOnClick()

        }

    }


}
