package com.gowtham.letschat.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.ImageView
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import coil.load
import coil.request.CachePolicy
import coil.transform.CircleCropTransformation
import com.gowtham.letschat.R
import com.gowtham.letschat.fragments.FImageSrcSheet
import com.gowtham.letschat.fragments.SheetListener
import com.theartofdev.edmodo.cropper.CropImage
import com.theartofdev.edmodo.cropper.CropImageView
import java.io.File

object ImageUtils {

    private const val FROM_GALLERY = 116

    private const val TAKE_PHOTO = 111

    private var photoUri: Uri? = null

    fun askPermission(context: Fragment) {
        if (checkStoragePermission(context))
            showCameraOptions(context)
    }

    fun loadUserImage(imageView: ImageView,imgUrl: String){
        imageView.load(imgUrl) {
            crossfade(true)
            crossfade(300)
            diskCachePolicy(CachePolicy.ENABLED)
            placeholder(R.drawable.ic_other_user)
            error(R.drawable.ic_other_user)
            transformations(CircleCropTransformation())
        }
    }

    private fun checkStoragePermission(context: Fragment): Boolean {
        return Utils.checkPermission(
            context, Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA
        )
    }

    private fun showCameraOptions(context: Fragment) {
        photoUri = null
        val builder = FImageSrcSheet(object :SheetListener{
            override fun selectedItem(index: Int) {
                if (index==0)
                    takePhoto(context.requireActivity())
                else
                    chooseGallery(context.requireActivity())
            }
        })
        builder.show(context.childFragmentManager,"")
    }

    private fun chooseGallery(context: Activity) {
        try {
            val intent = Intent(Intent.ACTION_PICK)
            intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*")
            context.startActivityForResult(intent, FROM_GALLERY)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun takePhoto(context: Activity) {
        val fileName = "Snap_" + System.currentTimeMillis() / 1000 + ".jpg"
        openCameraIntent(context, MediaStore.ACTION_IMAGE_CAPTURE, fileName, TAKE_PHOTO)
    }

    private fun openCameraIntent(
        context: Activity,
        action: String,
        fileName: String,
        reqCode: Int) {
        try {
            val intent = Intent(action)
            if (intent.resolveActivity(context.packageManager) != null) {
                val file = File(createImageFolder(context, ""), fileName)
                photoUri = if (isNougat())
                    FileProvider.getUriForFile(context, providerPath(context), file)
                else Uri.fromFile(file)
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
                context.startActivityForResult(intent, reqCode)
                context.overridePendingTransition(
                    android.R.anim.fade_in,
                    android.R.anim.fade_out
                )
            } else
                context.toast("Camera not available")
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    fun cropImage(context: Activity, data: Intent?, squareCrop: Boolean=true) {
        val imgUri: Uri? = getPhotoUri(data)
        imgUri?.let {
            val cropImage = CropImage.activity(imgUri)
                .setOutputCompressFormat(Bitmap.CompressFormat.JPEG)
                .setGuidelines(CropImageView.Guidelines.ON)
            if (squareCrop)
                cropImage.setAspectRatio(1, 1)
            cropImage.start(context)
        }
    }

    fun getCroppedImage(data: Intent?): Uri? {
        try {
            val result = CropImage.getActivityResult(data)
            return result?.uri
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
        return null
    }
    private fun getPhotoUri(data: Intent?): Uri? {
        return if (data == null || data.data == null) photoUri else data.data
    }

    private fun createImageFolder(context: Context, path: String): String? {
        val folderPath = context.getExternalFilesDir("")
            ?.absolutePath + "/" + context.getString(R.string.app_name)
        try {
            val file = File("$folderPath/$path")
            if (!file.exists()) file.mkdirs()
            return file.absolutePath
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
        return folderPath
    }

    private fun isNougat(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
    }

    private fun providerPath(context: Context): String {
        return context.packageName + ".fileprovider"
    }

    fun onImagePerResult(context: Fragment, vararg result: Int) {
        if (Utils.isPermissionOk(*result))
            showCameraOptions(context)
        else
            context.requireContext().toast(R.string.txt_file_p_error)
    }

}