package com.utf8coding.envGuardAdmin.activities

import android.animation.Animator
import android.content.Context
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.View.*
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.amap.api.location.AMapLocationClient
import com.amap.api.maps2d.AMap
import com.amap.api.maps2d.CameraUpdateFactory
import com.amap.api.maps2d.MapView
import com.amap.api.maps2d.model.*
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_HIDDEN
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.utf8coding.envGuardAdmin.MyApplication
import com.utf8coding.envGuardAdmin.R
import com.utf8coding.envGuardAdmin.adapter.MainRecyclerViewAdapter
import com.utf8coding.envGuardAdmin.data.BinData
import com.utf8coding.envGuardAdmin.viewModels.MainActivityViewModel
import com.utf8coding.envGuardAdmin.viewModels.MainActivityViewModel.Companion.DOUBLE_BACK_PRESS_DELAY
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent
import java.lang.Thread.sleep
import kotlin.concurrent.thread


class MainActivity : BaseActivity() {
    companion object{
        const val ADDING_BIN = 0
        const val EDITING_BIN = 1
        const val EMPTY = 2
    }

    private lateinit var viewModel: MainActivityViewModel
    private lateinit var binRecyclerView: RecyclerView
    private lateinit var mainRecyclerViewAdapter: MainRecyclerViewAdapter
    private lateinit var binDetailBottomSheet: FrameLayout
    private lateinit var binDetailBottomSheetConstraintLayout: ConstraintLayout
    private lateinit var mapView: MapView
    private var aMap: AMap? = null
    private var curMarker: Marker? = null
    private lateinit var longitudeTextField: TextInputLayout
    private lateinit var longitudeEditText: TextInputEditText
    private lateinit var latitudeTextField: TextInputLayout
    private lateinit var latitudeEditText: TextInputEditText
    private lateinit var levelTextField: TextInputLayout
    private lateinit var levelEditText: TextInputEditText
    private lateinit var descriptionTextField: TextInputLayout
    private lateinit var descriptionEditText: TextInputEditText
    private lateinit var statusTextField: TextInputLayout
    private lateinit var statusAutoText: AutoCompleteTextView
    private lateinit var detailTitleText: TextView
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<FrameLayout>
    private lateinit var commitButton: Button
    private lateinit var cancelButton: Button
    private lateinit var mailFab: ExtendedFloatingActionButton
    private lateinit var topAppBar: MaterialToolbar
    private lateinit var addBinLayout: ConstraintLayout
    private lateinit var addBinLongitudeEditText: TextInputEditText
    private lateinit var addBinLatitudeEditText: TextInputEditText
    private lateinit var addBinButton: Button
    private lateinit var cancelAddBinButton: Button
    private var descriptionY:Float? = null
    private lateinit var curBottomSheetData: BinData
    private  var bottomSheetStatus = EMPTY
    private var binDataList = ArrayList<BinData>()
    private var currentLat = 0.0
    private var currentLong = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        viewModel = ViewModelProvider(this)[MainActivityViewModel::class.java]

        initViews(savedInstanceState)
        initRecyclerView()
        getBinDataList()
    }

    override fun onBackPressed() {
        if (bottomSheetBehavior.state == STATE_EXPANDED){
            mailFab.show()
            bottomSheetBehavior.state = STATE_HIDDEN
        } else if (viewModel.firstBackPressedTime == 0L){
            //???????????????
            viewModel.firstBackPressedTime = System.currentTimeMillis()
            makeToast(getString(R.string.???????????????????????????))
        } else {
            val timeDifference = System.currentTimeMillis() - viewModel.firstBackPressedTime
            if (timeDifference > DOUBLE_BACK_PRESS_DELAY){
                //???????????????
                viewModel.firstBackPressedTime = 0L
            } else {
                ActivityCollector.finishAll()
            }
        }
    }

    private fun initViews(savedInstanceState: Bundle?){
        binDetailBottomSheetConstraintLayout = findViewById(R.id.constraintLayout1)
        binRecyclerView = findViewById(R.id.binRecyclerView)
        statusTextField = findViewById(R.id.statusTextField)
        statusAutoText = findViewById(R.id.statusAutoText)
        longitudeTextField = findViewById(R.id.longitudeTextField)
        longitudeEditText = findViewById(R.id.longitudeEditText)
        latitudeTextField = findViewById(R.id.latitudeTextField)
        latitudeEditText = findViewById(R.id.latitudeEditText)
        levelTextField = findViewById(R.id.levelTextField)
        levelEditText = findViewById(R.id.levelEditText)
        descriptionTextField = findViewById(R.id.descriptionTextField)
        descriptionEditText = findViewById(R.id.descriptionEditText)
        detailTitleText = findViewById(R.id.detailTitleText)
        binDetailBottomSheet = findViewById(R.id.binDetailBottomSheet)
        bottomSheetBehavior = BottomSheetBehavior.from(binDetailBottomSheet)
        mapView = findViewById(R.id.mapView)
        commitButton = findViewById(R.id.commitButton)
        cancelButton = findViewById(R.id.cancelButton)
        mailFab = findViewById(R.id.mailFab)
        topAppBar = findViewById(R.id.topAppBar)
        addBinLayout = findViewById(R.id.addBinLayout)
        addBinLongitudeEditText = findViewById(R.id.addBinLongitudeEditText)
        addBinLatitudeEditText = findViewById(R.id.addBinLatitudeEditText)
        addBinButton = findViewById(R.id.addBinButton)
        cancelAddBinButton = findViewById(R.id.cancelAddBinButton)


        //????????????????????????
        val items = listOf("??????", "??????")
        val adapter = ArrayAdapter(this, R.layout.list_item, items)
        (statusTextField.editText as? AutoCompleteTextView)?.setAdapter(adapter)

        //bottomSheet?????????
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        bottomSheetBehavior.isDraggable = false

        //??????????????????????????????????????????
        binDetailBottomSheetConstraintLayout.setOnClickListener {
            thread {
                runOnUiThread{
                    statusAutoText.focusable = NOT_FOCUSABLE
                }
                sleep(2)
                runOnUiThread{
                    statusAutoText.focusable = FOCUSABLE
                    statusAutoText.clearFocus()
                    statusAutoText.dismissDropDown()
                    longitudeEditText.clearFocus()
                    latitudeEditText.clearFocus()
                    levelEditText.clearFocus()
                    descriptionEditText.clearFocus()
                }
            }
            val manager: InputMethodManager =
                applicationContext.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            manager.hideSoftInputFromWindow(
                currentFocus?.windowToken,
                InputMethodManager.HIDE_NOT_ALWAYS
            )
        }

        //??????????????????
        AMapLocationClient.updatePrivacyShow(this, true, true)
        AMapLocationClient.updatePrivacyAgree(this, true)
        mapView.onCreate(savedInstanceState)
        if (aMap == null) {
            aMap = mapView.map
        }
        val myLocationStyle = MyLocationStyle()
        myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_FOLLOW_NO_CENTER)
        myLocationStyle.showMyLocation(true)
        myLocationStyle.interval(2000)
        aMap!!.setMyLocationStyle(myLocationStyle)
        aMap!!.isMyLocationEnabled = true
        aMap!!.setOnMyLocationChangeListener { location ->
            currentLat = location.latitude; currentLong = location.longitude
            Log.i("MainActivity:", "long: ${location.longitude}, lat: ${location.latitude}")
            mainRecyclerViewAdapter.refreshLocation(currentLat, currentLong)
        }

        //???????????????
        commitButton.setOnClickListener {
            onCommit()
        }
        cancelButton.setOnClickListener {
            mailFab.show()
            bottomSheetBehavior.state = STATE_HIDDEN
        }
        mailFab.setOnClickListener {
            viewModel.sendMail()
            Toast.makeText(this, "???????????????????????????", Toast.LENGTH_SHORT).show()
        }

        //top appbar menu
        topAppBar.setOnMenuItemClickListener {
            if (it.itemId == R.id.addBin && bottomSheetBehavior.state == STATE_HIDDEN){
                onAddBin()
            } else if (bottomSheetBehavior.state == STATE_EXPANDED){
                Toast.makeText(this, "????????????????????????????????????????????????", Toast.LENGTH_SHORT).show()
            }
            true
        }
    }

    private fun getBinDataList(){
        viewModel.binList.observe(this) {
            binDataList.clear()
            viewModel.binList.value?.let { it1 -> binDataList.addAll(it1) }
            mainRecyclerViewAdapter.mNotifyDataSetChanged()
        }
        viewModel.getBinDataList(
            onChange = {
                //todo ??????onChange???observe???????????????????????????
                binDataList.clear()
                viewModel.binList.value?.let { it1 -> binDataList.addAll(it1) }
                mainRecyclerViewAdapter.mNotifyDataSetChanged()
            },
            onFailure = {
                Toast.makeText(this, resources.getString(R.string.???????????????????????????), Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun initRecyclerView(){
        mainRecyclerViewAdapter = MainRecyclerViewAdapter(binDataList) {
            onBinListItemClick(it)
        }
        binRecyclerView.adapter = mainRecyclerViewAdapter
        binRecyclerView.layoutManager = LinearLayoutManager(this)
    }

    private fun onBinListItemClick(binData: BinData){
        mailFab.hide()

        //????????????marker????????????????????????????????????
        curMarker?.remove()
        val latLng = LatLng(binData.latitude, binData.longitude)
        curMarker = aMap!!.addMarker(
            MarkerOptions()
                .position(latLng)
                .title("${binData.id} ????????????")
                .icon(
                    BitmapDescriptorFactory.fromBitmap(
                        BitmapFactory.decodeResource(resources, R.drawable.ic_bin)
                    )
                )
        )
        aMap!!.animateCamera(
            CameraUpdateFactory.newCameraPosition(CameraPosition(latLng, 18f, 0f, 0f)),
            500,
            null
        )

        //?????????????????????????????????
        if (descriptionY == null){
            descriptionY = descriptionTextField.y
        }

        //?????????bottomSheet????????????
        bottomSheetStatus = EDITING_BIN
        curBottomSheetData = binData
        descriptionTextField.y = descriptionY as Float
        mapView.alpha = 1f

        //???????????????????????????
        KeyboardVisibilityEvent.setEventListener(this) {
            if (!it) {
                longitudeEditText.clearFocus()
                latitudeEditText.clearFocus()
                levelEditText.clearFocus()
                descriptionEditText.clearFocus()
                addBinLatitudeEditText.clearFocus()
                addBinLongitudeEditText.clearFocus()
            }
        }

        //????????????????????????
        binData.let{ b ->
//            if (b.state == 1){
//                statusAutoText.setText("??????")
//            } else {
//                statusAutoText.setText("??????")
//            }
            statusAutoText.setOnItemClickListener { _, _, _, _ ->
                statusAutoText.clearFocus()
            }
            detailTitleText.text = "${b.id}??????????????????"
            longitudeEditText.setText(b.longitude.toString())
            latitudeEditText.setText(b.latitude.toString())
            levelEditText.setText((b.level * 100).toInt().toString())
            levelEditText.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus){
                    try {
                        val newLevel = levelEditText.text.toString().toInt()
                        if (newLevel < 0 || newLevel > 100){
                            levelEditText.setText((b.level * 100).toString())
                            levelTextField.errorContentDescription = "0 - 100"
                        } else {
                            levelTextField.errorContentDescription = null
                        }
                    } catch (e: NumberFormatException){
                        levelEditText.setText((b.level * 100).toString())
                        levelTextField.errorContentDescription = "0 - 100"
                    }
                }
            }
            longitudeEditText.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus){
                    try {
                        longitudeEditText.text.toString().toDouble()
                    } catch (e: NumberFormatException){
                        longitudeEditText.setText(b.longitude .toString())
                    }
                }
            }
            latitudeEditText.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus){
                    try {
                        latitudeEditText.text.toString().toDouble()
                    } catch (e: NumberFormatException){
                        latitudeEditText.setText(b.latitude .toString())
                    }
                }
            }
            descriptionEditText.setText(b.description)
            descriptionEditText.setOnFocusChangeListener { view, onFocus ->
                if (onFocus){
                    descriptionTextField.animate().yBy(-620f)
                    mapView.animate().alpha(0f)
                    commitButton.animate().alpha(0f)
                    commitButton.isClickable = false
                    cancelButton.animate().alpha(0f)
                    cancelButton.isClickable = false
                } else {
                    descriptionTextField.animate().y(descriptionY!!)
                    mapView.animate().alpha(1f)
                    commitButton.animate().alpha(1f)
                    commitButton.isClickable = true
                    cancelButton.animate().alpha(1f)
                    cancelButton.isClickable = true
                }
            }
        }

        //??????????????????
        bottomSheetBehavior.state = STATE_EXPANDED
        addBinLayout.animate().alpha(0f)
            .setListener(OnEndAnimationListener{
                addBinLayout.visibility = GONE
                addBinLayout.animate().setListener(OnEndAnimationListener{})
            })
    }

    private fun onAddBin(){
        addBinLayout.visibility = VISIBLE
        addBinLayout.animate().alpha(1f)
        addBinButton.setOnClickListener {
            var newLat: Double? = null
            var newLong: Double? = null
            try {
                newLat = addBinLongitudeEditText.text.toString().toDouble()
            } catch (e: NumberFormatException){
                longitudeEditText.setText("")
                Toast.makeText(this, "????????????????????????", Toast.LENGTH_SHORT).show()
            }
            try {
                newLong = addBinLatitudeEditText.text.toString().toDouble()
            } catch (e: NumberFormatException){
                latitudeEditText.setText("")
                Toast.makeText(this, "????????????????????????", Toast.LENGTH_SHORT).show()
            }
            if (newLat != null && newLong != null){
                viewModel.addBin(
                    newLat, newLong,
                    {
                        Toast.makeText(this, "????????????", Toast.LENGTH_SHORT).show()
                    }, {
                        Toast.makeText(this, "????????????????????????", Toast.LENGTH_SHORT).show()
                    }
                )
            }
            addBinLayout.animate().alpha(0f)
                .setListener(OnEndAnimationListener{
                    addBinLayout.visibility = GONE
                    addBinLayout.animate().setListener(OnEndAnimationListener{})
                })
            viewModel.getBinDataList(
                {
                    //todo ??????onChange???observe???????????????????????????
                    binDataList.clear()
                    viewModel.binList.value?.let { it1 -> binDataList.addAll(it1) }
                    mainRecyclerViewAdapter.mNotifyDataSetChanged()
                },
                {
                    Toast.makeText(this, resources.getString(R.string.???????????????????????????), Toast.LENGTH_SHORT).show()
                }
            )
        }
        cancelAddBinButton.setOnClickListener {
            addBinLayout.animate().alpha(0f)
                .setListener(OnEndAnimationListener{
                    addBinLayout.visibility = GONE
                    addBinLayout.animate().setListener(OnEndAnimationListener{})
                })
            longitudeEditText.setText("")
            latitudeEditText.setText("")
        }
    }

    private fun onCommit(){
        var newLat: Double = try {
            addBinLongitudeEditText.text.toString().toDouble()
        } catch (e: NumberFormatException){
            curBottomSheetData.latitude
        }
        var newLong: Double = try {
            addBinLatitudeEditText.text.toString().toDouble()
        } catch (e: NumberFormatException){
            curBottomSheetData.longitude
        }
        viewModel.commitData(
            BinData(
                curBottomSheetData.id,
                newLat,
                newLong,
                levelEditText.text.toString().toFloat() / 100,
                if (statusAutoText.text.toString() == "??????") 0 else 1,
                descriptionEditText.text.toString()
            ),
            onSuccess = {
                mailFab.show()
                bottomSheetBehavior.state = STATE_HIDDEN
                Toast.makeText(this, "??????????????????", Toast.LENGTH_SHORT).show()
                viewModel.getBinDataList(
                        {
                            //todo ??????onChange???observe???????????????????????????
                            binDataList.clear()
                            viewModel.binList.value?.let { it1 -> binDataList.addAll(it1) }
                            mainRecyclerViewAdapter.mNotifyDataSetChanged()
                        },
                {
                    Toast.makeText(this, resources.getString(R.string.???????????????????????????), Toast.LENGTH_SHORT).show()
                }
                )
            },
            onFailure = {
                Toast.makeText(this, "??????????????????????????????", Toast.LENGTH_SHORT).show()
            }
        )

    }

    //tools:
    private fun makeToast(msg: String){
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
    private fun makeILog(msg: String){
        Log.i("MainActivity:", msg)
    }

    open inner class OnEndAnimationListener(private val mOnAnimationEnd: (animation: Animator?) -> Unit): Animator.AnimatorListener {
        override fun onAnimationStart(animation: Animator?) {}
        override fun onAnimationEnd(animation: Animator?) {
            mOnAnimationEnd(animation)
        }
        override fun onAnimationCancel(animation: Animator?) {}
        override fun onAnimationRepeat(animation: Animator?) {}
    }
}