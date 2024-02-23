package com.programminghut.realtime_object

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.*
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.view.Surface
import android.view.TextureView
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.programminghut.realtime_object.ml.SsdMobilenetV11Metadata1
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.image.ops.ResizeWithCropOrPadOp

class MainActivity : AppCompatActivity() {

    lateinit var labels:List<String>
    var colors = listOf<Int>(
        Color.BLUE, Color.GREEN, Color.RED, Color.CYAN, Color.GRAY, Color.BLACK,
        Color.DKGRAY, Color.MAGENTA, Color.YELLOW, Color.RED)
    val paint = Paint()

    lateinit var imageProcessor: ImageProcessor
    lateinit var bitmap:Bitmap
    lateinit var imageView: ImageView
    lateinit var cameraDevice: CameraDevice
    lateinit var handler: Handler
    lateinit var cameraManager: CameraManager
    lateinit var textureView: TextureView
    lateinit var model:SsdMobilenetV11Metadata1


    // This is the main function of the app

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Calls the function that 
        get_permission()

        // Loads the labels for the object detection from the labels.txt file
        labels = FileUtil.loadLabels(this, "labels.txt")

        // Configuration of the image Processor that resize the image to a 320x320 resolution to suit the AI model for Object detection
        // This image processor will be applied to the live camera feed
        imageProcessor = ImageProcessor.Builder().add(ResizeOp(320,320,ResizeOp.ResizeMethod.BILINEAR))
            .add(NormalizeOp(0f, 255f))
            .build()

        // This line selects the AI model to use and create a new Instance 
        model = SsdMobilenetV11Metadata1.newInstance(this)

        // Starts a handler looper to handle asynchronous actions
        val handlerThread = HandlerThread("videoThread")
        handlerThread.start()        
        handler = Handler(handlerThread.looper)

        // Initializes an imageView to a imageView on the app screen activity layout
        imageView = findViewById(R.id.imageView)

        // Same for the textureView on the layout
        textureView = findViewById(R.id.textureView)

        // This configures the surfaceTextureListener to call open_camera() when the surface TextureView becomes available for rendering
        textureView.surfaceTextureListener = object:TextureView.SurfaceTextureListener{
            override fun onSurfaceTextureAvailable(p0: SurfaceTexture, p1: Int, p2: Int) {
                open_camera()
            }
            override fun onSurfaceTextureSizeChanged(p0: SurfaceTexture, p1: Int, p2: Int) {
            }

            override fun onSurfaceTextureDestroyed(p0: SurfaceTexture): Boolean {
                return false
            }



            // This section of code is executed each time the surfaceTexture is updated
            override fun onSurfaceTextureUpdated(p0: SurfaceTexture) {

            	// This takes the image "textureView" captured by the camera and converts it to a bitmap
                bitmap = textureView.bitmap!!

                // Initializes a image to the format TensorImage to accepts the bitmap previously captured
                var image = TensorImage.fromBitmap(bitmap)

                // Applies the image processing to the image to suit the AI model input needed
                image = imageProcessor.process(image)

                // Sends the bitmag image to the AI model and fetch the results to the output features variable
                val outputs = model.process(image.tensorBuffer)

                // Stores the different outputed features into the following variables as floatArrays
                val locations = outputs.outputFeature1AsTensorBuffer.floatArray
                val classes = outputs.outputFeature3AsTensorBuffer.floatArray
                val scores = outputs.outputFeature0AsTensorBuffer.floatArray

                // Thiw creates a mutable copy of the bitmap inputed image 
                var mutable = bitmap.copy(Bitmap.Config.ARGB_8888, true)

                // This creates a 2D drawing surface and tells it to draw directly on the pixels of the bitmap
                val canvas = Canvas(mutable)

                // Configures the mutable canvos to size in order to draw on it
                val h = mutable.height
                val w = mutable.width
                paint.textSize = h/15f
                paint.strokeWidth = h/85f
                var x = 0

                // Draws the bounding boxes of the detected objects and display the name of the label and the accuracy float
                // These are drawn directly on the processed preview of the camera feed
                scores.forEachIndexed { index, fl ->
                    x = index
                    x *= 4
                    if(fl > 0.5){
                        paint.setColor(colors.get(index))
                        paint.style = Paint.Style.STROKE
                        canvas.drawRect(RectF(locations.get(x+1)*w,
                        locations.get(x)*h,
                        locations.get(x+3)*w,
                        locations.get(x+2)*h),
                        paint)
                        paint.style = Paint.Style.FILL
                        canvas.drawText(labels.get(classes.get(index).toInt())+" "+fl.toString(), locations.get(x+1)*w, locations.get(x)*h, paint)
                    }
                }

                // Set the processed bitmap with bounding boxes detection on the imageView of the layout
                // This displays the results and the previes on the screen of the smartphone
                imageView.setImageBitmap(mutable)


            }
        }

        // This retireves the CAMERA SERVICE to access to the devices's camera functionality
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager

    }


    // This function destroys the handler of the AI model

    override fun onDestroy() {
        super.onDestroy()
        model.close()
    }


    // This function opens the camera of the smartphone and redirects the feed to the surface texture View
    // This is done to display the camera feed on the screen of the smartphone

    @SuppressLint("MissingPermission")
    fun open_camera(){

    	// Creates the camera handler with the rear facing camera
        cameraManager.openCamera(cameraManager.cameraIdList[0], object:CameraDevice.StateCallback(){
            override fun onOpened(p0: CameraDevice) {
                cameraDevice = p0

                // Creates the surfacteTexture from the textureView
                var surfaceTexture = textureView.surfaceTexture

                // Attach the surfaceTexture to a Surface
                var surface = Surface(surfaceTexture)

                // Create a capture request to get the preview feed
                var captureRequest = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                captureRequest.addTarget(surface)

                // Create a capture session with the configured camera and displays the feed on the Surface Preview View
                cameraDevice.createCaptureSession(listOf(surface), object: CameraCaptureSession.StateCallback(){
                    override fun onConfigured(p0: CameraCaptureSession) {
                        p0.setRepeatingRequest(captureRequest.build(), null, null)
                    }
                    override fun onConfigureFailed(p0: CameraCaptureSession) {
                    }
                }, handler)
            }

            override fun onDisconnected(p0: CameraDevice) {

            }

            override fun onError(p0: CameraDevice, p1: Int) {

            }
        }, handler)
    }


    // This function requests the permission that the app needs in order to use the camera of the smartphone

    fun get_permission(){
        if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
            requestPermissions(arrayOf(android.Manifest.permission.CAMERA), 101)
        }
    }

    // 

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(grantResults[0] != PackageManager.PERMISSION_GRANTED){
            get_permission()
        }
    }
}