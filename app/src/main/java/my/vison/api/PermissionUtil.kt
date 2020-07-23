package my.vison.api

import android.app.Activity
import android.content.pm.PackageManager
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import kotlin.collections.ArrayList

class PermissionUtil {

    // vararg : 가변 인수
    fun requestPermission(activity: Activity, requestCode: Int, vararg permission: String): Boolean {

        var granted = true
        val permissionNeeded = ArrayList<String>()

        permission.forEach {
            // forEach로 도는 it이란건 permission 배열 안의
            // [Manifest.permisson.CAMERA][Manifest.permisson.READ_EXTERNAL_STORAGE]를 뜻하고
            // 각 요청의 Boolean값을 permissionCheck 에 넣어 각각 GRANTED 되었는지 비교한다
            val permissionCheck = ContextCompat.checkSelfPermission(activity, it)
            val hasPermission = permissionCheck == PackageManager.PERMISSION_GRANTED
            granted = granted and hasPermission
            // 만약 GRANT 되지 않은 permissionCheck가 있다면 그것을 permissionNeeded라는
            // ArrayList에 담아 후에 이 ArrayList를 통해서 해당 권한을 얻는 작업을 수행한다
            if (!hasPermission) {
                permissionNeeded.add(it)
            }
        }

        if (granted) {
            return true
        }else {
            // GRANTED되지 않은 값이 있다면 여기서 해당 권한을 얻는 작업을 수행한다
            // toTypedArray는 ArrayList를 Array 타입으로 바꿔줌
            ActivityCompat.requestPermissions(
                activity, permissionNeeded.toTypedArray(), requestCode)
            return false
        }
    }

    // 권한이 존재하는지 확인하는 함수로 onRequestPermissionsResult() 내부에서 사용한다
    // Intent 수행시 보낸 requestCode와 requestPermission()에서 보내 권한을 얻으려한 requestCode가 맞는지 확인한다
    // grantResults는 권한의 허용,거부 내용을 담은 Array로
    // 해당 Array가 존재하지는지(권한을 요청한 적이 있는지)
    // 해당 권한을 얻었는지 확인해서 Boolean타입으로 return한다
    fun permissionGranted(requestCode : Int, permissionCode : Int, grantResults : IntArray) : Boolean{

        return requestCode == permissionCode
                // 획득된 권한이 있는지 확인
                && grantResults.size > 0
                // 첫 권한이 획득 되었는지 확인
                && grantResults[0] == PackageManager.PERMISSION_GRANTED
    }


}