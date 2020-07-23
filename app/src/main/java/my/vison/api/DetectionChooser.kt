package my.vison.api

import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.*
import kotlinx.android.synthetic.main.detection_chooser.*

class DetectionChooser : DialogFragment() {

    private var detectionChooserNotifierInterface : DetectionChooserNotifierInterface? = null

    interface DetectionChooserNotifierInterface{
        fun detectLable()
        fun detectLanmark()
        fun detectText()
        fun detectCancle()
    }

    override fun onCreateView(inflater: LayoutInflater,container: ViewGroup?,savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.detection_chooser,container,false)

    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setupListener()
    }

    fun addDetectionChooserNotifierInterface(listener : DetectionChooserNotifierInterface) {
        detectionChooserNotifierInterface = listener
    }

    private fun setupListener() {
        detect_label.setOnClickListener {
            detectionChooserNotifierInterface?.detectLable()
            dismiss()
        }
        detect_landmark.setOnClickListener {
            detectionChooserNotifierInterface?.detectLanmark()
            dismiss()
        }
        detect_text.setOnClickListener {
            detectionChooserNotifierInterface?.detectText()
            dismiss()
        }
        detect_cancle.setOnClickListener {
            detectionChooserNotifierInterface?.detectCancle()
            dismiss()
        }
    }

}