/*
package com.example.rfidstockpro.ui.fragments

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.os.SystemClock
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.util.SparseBooleanArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView.OnItemClickListener
import android.widget.AdapterView.OnItemLongClickListener
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.ListView
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.rfidstockpro.R
import com.example.rfidstockpro.Utils.Utils
import com.example.rfidstockpro.Utils.ViewUtils.setViewAlpha
import com.example.rfidstockpro.tools.NumberTool
import com.example.rfidstockpro.ui.activities.DashboardActivity
import com.example.rfidstockpro.ui.activities.DashboardActivity.IConnectStatus
import com.rscja.deviceapi.RFIDWithUHFBLE
import com.rscja.deviceapi.entity.InventoryParameter
import com.rscja.deviceapi.entity.UHFTAGInfo
import com.rscja.deviceapi.interfaces.ConnectionStatus
import com.rscja.deviceapi.interfaces.KeyEventCallback

class UHFReadTagFragment : Fragment() {
    private val TAG = "UHFReadTagFragment"
    private var mContext: DashboardActivity? = null

    var lastIndex: Int = -1
    private var LvTags: ListView? = null
    private var btnSingleInventory: Button? = null
    private var btClear: Button? = null
    private var tv_count: TextView? = null
    private var tv_total: TextView? = null
    private var tv_time: TextView? = null
    var etTime: EditText? = null
    private var cbPhase: CheckBox? = null
    private var btStop: TextView? = null
    private var btStartScan: TextView? = null
    private var btCancel: TextView? = null

    private var isExit = false
    private var total: Long = 0
    private var useTime = 0.0
    private var adapter: MyAdapter? = null

    private val tagList: MutableList<UHFTAGInfo> = ArrayList()
    private val mConnectStatus: ConnectStatus = ConnectStatus()
    var maxRunTime: Int = 999999

    //--------------------------------------获取 解析数据-------------------------------------------------
    val FLAG_START: Int = 0 //开始
    val FLAG_STOP: Int = 1 //停止
    val FLAG_UPDATE_TIME: Int = 2 // 更新时间
    val FLAG_UHFINFO: Int = 3
    val FLAG_SUCCESS: Int = 10 //成功
    val FLAG_FAIL: Int = 11 //失败
    val FLAG_TIME_OVER: Int = 12 //
    private var mStrTime: Long = 0

    var handler: Handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                FLAG_TIME_OVER -> {
                    Log.i(TAG, "FLAG_TIME_OVER =" + (System.currentTimeMillis() - mStrTime))
                    useTime = NumberTool.getPointDouble(
                        1,
                        ((System.currentTimeMillis() - mStrTime) / 1000.0f).toDouble()
                    )
                    tv_time!!.text = useTime.toString() + "s"
                    stop()
                }

                FLAG_STOP -> if (msg.arg1 == FLAG_SUCCESS) {
                    //停止成功
                    btClear!!.isEnabled = true
                    btStop!!.isEnabled = false
                    setViewAlpha(btStop!!, 0.3f)
                    btStartScan!!.isEnabled = true
                    setViewAlpha(btStartScan!!, 1f)
                    btnSingleInventory!!.isEnabled = true
                } else {
                    //停止失败
                    Utils.playSound(2)
                    Toast.makeText(
                        mContext,
                        R.string.uhf_msg_inventory_stop_fail,
                        Toast.LENGTH_SHORT
                    ).show()
                }

                FLAG_START -> if (msg.arg1 == FLAG_SUCCESS) {
                    //开始读取标签成功
//                        btClear.setEnabled(false);
                    btStop!!.isEnabled = true
                    setViewAlpha(btStop!!, 1f)
                    btStartScan!!.isEnabled = false
                    setViewAlpha(btStartScan!!, 0.3f)
                    btnSingleInventory!!.isEnabled = false
                } else {
                    //开始读取标签失败
                    Utils.playSound(2)
                }

                FLAG_UPDATE_TIME -> if (mContext!!.isScanning) {
                    useTime = NumberTool.getPointDouble(
                        1,
                        ((System.currentTimeMillis() - mStrTime) / 1000.0f).toDouble()
                    )
                    tv_time!!.text = useTime.toString() + "s"
                    sendEmptyMessageDelayed(FLAG_UPDATE_TIME, 10)
                } else {
                    removeMessages(FLAG_UPDATE_TIME)
                }

                FLAG_UHFINFO -> {
                    val info = msg.obj as UHFTAGInfo
                    addTagToList(info)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_uhfread_tag, container, false)
        initFilter(view)

        if (activity is DashboardActivity) {
            (activity as DashboardActivity).updateToolbarTitle("RFID Tags")
        }

        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        Log.i(TAG, "UHFReadTagFragment.onActivityCreated")
        super.onActivityCreated(savedInstanceState)
        lastIndex = -1
        mContext = activity as DashboardActivity?
        init()
        selectIndex = -1
        mContext!!.selectEPC = null
    }

    override fun onPause() {
        super.onPause()
        stop()
    }

    override fun onDestroyView() {
        Log.i(TAG, "UHFReadTagFragment.onDestroyView")
        super.onDestroyView()
        isExit = true
        mContext!!.uhf.setInventoryCallback(null)
        mContext!!.uhf.setKeyEventCallback(null)
        mContext!!.removeConnectStatusNotice(mConnectStatus)
    }


    private fun init() {
        isExit = false
        mContext!!.addConnectStatusNotice(mConnectStatus)
        LvTags = requireView().findViewById<View>(R.id.LvTags) as ListView
        btnSingleInventory = requireView().findViewById<View>(R.id.btnSingleInventory) as Button
        btStartScan = requireView().findViewById<View>(R.id.btStartScan) as TextView
        btStop = requireView().findViewById<View>(R.id.btStop) as TextView
        btClear = requireView().findViewById<View>(R.id.btClear) as Button
        tv_count = requireView().findViewById<View>(R.id.tv_count) as TextView
        tv_total = requireView().findViewById<View>(R.id.tv_total) as TextView
        tv_time = requireView().findViewById<View>(R.id.tv_time) as TextView
        etTime = requireView().findViewById<View>(R.id.etTime) as EditText
        btCancel = requireView().findViewById<View>(R.id.btCancel) as TextView
        cbPhase = requireView().findViewById(R.id.cbPhase)

        btnSingleInventory!!.setOnClickListener { v: View? -> singleInventory() }
        btStartScan!!.setOnClickListener { v: View? -> startInventory() }
        btStop!!.setOnClickListener { v: View? -> stop() }
        btClear!!.setOnClickListener { v: View? -> clearData() }
        btCancel!!.setOnClickListener { v: View? -> goBack() }
        tv_count!!.text = tagList.size.toString()
        tv_total!!.text = total.toString()
        tv_time!!.text = useTime.toString() + "s"

        setViewAlpha(btStop!!, 0.3f)
        setViewAlpha(btStartScan!!, 1f)
        btStartScan!!.isEnabled = true

        mContext!!.tagList = tagList

        adapter = MyAdapter(mContext)
        LvTags!!.adapter = adapter
        handler.postDelayed({
            mContext!!.uhf.setKeyEventCallback(object : KeyEventCallback {
                override fun onKeyDown(keycode: Int) {
                    Log.d(TAG, "  keycode =$keycode   ,isExit=$isExit")
                    if (!isExit && mContext!!.uhf.connectStatus == ConnectionStatus.CONNECTED) {
                        if (keycode == 3) {
                            mContext!!.isKeyDownUP = true
                            startInventory()
                        } else {
                            if (!mContext!!.isKeyDownUP) {
                                if (keycode == 1) {
                                    if (mContext!!.isScanning) {
                                        stop()
                                    } else {
                                        startInventory()
                                    }
                                }
                            }
                            if (keycode == 2) {
                                if (mContext!!.isScanning) {
                                    stop()
                                    SystemClock.sleep(100)
                                }
                                //MR20
                                singleInventory()
                            }
                        }
                    }
                }

                override fun onKeyUp(keycode: Int) {
                    Log.d(TAG, "  keycode =$keycode   ,isExit=$isExit")
                    if (keycode == 4) {
                        stop()
                    }
                }
            })
        }, 200)
        LvTags!!.onItemClickListener =
            OnItemClickListener { parent, view, position, id ->
                selectIndex = position
                adapter!!.notifyDataSetInvalidated()
                mContext!!.selectEPC = tagList[position].epc
                Toast.makeText(view.context, "clicked", Toast.LENGTH_SHORT).show()
            }
        LvTags!!.onItemLongClickListener =
            OnItemLongClickListener { adapterView, view, position, l ->
                val clipboard =
                    view.context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText(
                    "label", mContext!!.tagList!!.get(position).getExtraData(
                        KEY_TAG
                    )
                )
                clipboard.setPrimaryClip(clip)
                Toast.makeText(view.context, R.string.msg_copy_clipboard, Toast.LENGTH_SHORT).show()
                false
            }
        //        clearData();
    }

    private fun goBack() {
        Log.e("GoBackTAG", "goBack: ")
        if (activity != null) {
            requireActivity().supportFragmentManager.popBackStack()
        }
    }

    private var cbFilter: CheckBox? = null
    private var layout_filter: ViewGroup? = null
    private var btnSetFilter: Button? = null

    private fun initFilter(view: View) {
        layout_filter = view.findViewById<View>(R.id.layout_filter) as ViewGroup
        layout_filter!!.visibility = View.GONE
        cbFilter = view.findViewById<View>(R.id.cbFilter) as CheckBox
        cbFilter!!.setOnCheckedChangeListener { buttonView: CompoundButton?, isChecked: Boolean ->
            layout_filter!!.visibility =
                if (isChecked) View.VISIBLE else View.GONE
        }

        val etLen = view.findViewById<View>(R.id.etLen) as EditText
        val etPtr = view.findViewById<View>(R.id.etPtr) as EditText
        val etData = view.findViewById<View>(R.id.etData) as EditText
        val rbEPC = view.findViewById<View>(R.id.rbEPC) as RadioButton
        val rbTID = view.findViewById<View>(R.id.rbTID) as RadioButton
        val rbUser = view.findViewById<View>(R.id.rbUser) as RadioButton

        etData.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
            }

            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
            }

            override fun afterTextChanged(editable: Editable) {
                etLen.setText((etData.text.toString().length * 4).toString())
            }
        })

        btnSetFilter = view.findViewById<View>(R.id.btSet) as Button
        btnSetFilter!!.setOnClickListener(View.OnClickListener {
            var filterBank = RFIDWithUHFBLE.Bank_EPC
            if (rbEPC.isChecked) {
                filterBank = RFIDWithUHFBLE.Bank_EPC
            } else if (rbTID.isChecked) {
                filterBank = RFIDWithUHFBLE.Bank_TID
            } else if (rbUser.isChecked) {
                filterBank = RFIDWithUHFBLE.Bank_USER
            }
            etLen.text.toString()
            if (etLen.text.toString().isEmpty()) {
                //                    showToast("数据长度不能为空");
                Toast.makeText(requireActivity(), "数据长度不能为空", Toast.LENGTH_SHORT).show()

                return@OnClickListener
            }
            etPtr.text.toString()
            if (etPtr.text.toString().isEmpty()) {
                //                    showToast("起始地址不能为空");
                Toast.makeText(requireActivity(), "起始地址不能为空", Toast.LENGTH_SHORT).show()

                return@OnClickListener
            }
            val ptr = Utils.toInt(etPtr.text.toString(), 0)
            val len = Utils.toInt(etLen.text.toString(), 0)
            var data = etData.text.toString().trim { it <= ' ' }
            if (len > 0 && !TextUtils.isEmpty(data)) {
                val rex = "[\\da-fA-F]*" //匹配正则表达式，数据为十六进制格式
                if (!data.matches(rex.toRegex())) {
                    //                        mContext.showToast("过滤的数据必须是十六进制数据");
                    Toast.makeText(
                        requireActivity(),
                        "过滤的数据必须是十六进制数据",
                        Toast.LENGTH_SHORT
                    ).show()
                    //                        mContext.playSound(2);
                    return@OnClickListener
                }

                val l = data.replace(" ", "").length
                if (len <= l * 4) {
                    if (l % 2 != 0) data += "0"
                } else {
                    //                        mContext.showToast(R.string.uhf_msg_set_filter_fail2);
                    Toast.makeText(
                        requireActivity(),
                        R.string.uhf_msg_set_filter_fail2,
                        Toast.LENGTH_SHORT
                    ).show()
                    return@OnClickListener
                }

                if (mContext!!.uhf.setFilter(filterBank, ptr, len, data)) {
                    //                        mContext.showToast(R.string.uhf_msg_set_filter_succ);
                    Toast.makeText(
                        requireActivity(),
                        R.string.uhf_msg_set_filter_succ,
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    //                        mContext.showToast(R.string.uhf_msg_set_filter_fail);
                    Toast.makeText(
                        requireActivity(),
                        R.string.uhf_msg_set_filter_fail,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                //禁用过滤
                val dataStr = "00"
                if (mContext!!.uhf.setFilter(RFIDWithUHFBLE.Bank_EPC, 0, 0, dataStr)
                    && mContext!!.uhf.setFilter(RFIDWithUHFBLE.Bank_TID, 0, 0, dataStr)
                    && mContext!!.uhf.setFilter(RFIDWithUHFBLE.Bank_USER, 0, 0, dataStr)
                ) {
                    //                        mContext.showToast(R.string.msg_disable_succ);
                    Toast.makeText(requireActivity(), R.string.msg_disable_succ, Toast.LENGTH_SHORT)
                        .show()
                } else {
                    //                        mContext.showToast(R.string.msg_disable_fail);
                    Toast.makeText(requireActivity(), R.string.msg_disable_fail, Toast.LENGTH_SHORT)
                        .show()
                }
            }
            cbFilter!!.isChecked = true
        })

        rbEPC.setOnClickListener {
            if (rbEPC.isChecked) {
                etPtr.setText("32")
            }
        }
        rbTID.setOnClickListener {
            if (rbTID.isChecked) {
                etPtr.setText("0")
            }
        }
        rbUser.setOnClickListener {
            if (rbUser.isChecked) {
                etPtr.setText("0")
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (mContext!!.uhf.connectStatus == ConnectionStatus.CONNECTED) {
            btStartScan!!.isEnabled = true
            setViewAlpha(btStartScan!!, 1f)
            btnSingleInventory!!.isEnabled = true
            cbFilter!!.isEnabled = true
        } else {
            btStartScan!!.isEnabled = false
            setViewAlpha(btStartScan!!, 0.3f)
            btnSingleInventory!!.isEnabled = false
            cbFilter!!.isChecked = false
            cbFilter!!.isEnabled = false
        }
    }

    private fun clearData() {
        total = 0
        tv_count!!.text = "0"
        tv_total!!.text = "0"
        tv_time!!.text = "0s"
        tagList.clear()
        mContext!!.selectEPC = null
        selectIndex = -1
        adapter!!.notifyDataSetChanged()
    }

    */
/**
     * 停止识别
     *//*

    private fun stop() {
        Log.i(TAG, "stop mContext.isScanning=false")
        handler.removeMessages(FLAG_TIME_OVER)
        if (mContext!!.isScanning) {
            stopInventory()
        }
        mContext!!.isScanning = false
    }

    private fun stopInventory() {
        //Log.i(TAG, "stopInventory() 2");
        val connectionStatus = mContext!!.uhf.connectStatus
        if (connectionStatus != ConnectionStatus.CONNECTED) {
            return
        }
        var result = false
        result = mContext!!.uhf.stopInventory()
        val msg = handler.obtainMessage(FLAG_STOP)
        if (!result && connectionStatus == ConnectionStatus.CONNECTED) {
            msg.arg1 = FLAG_FAIL
        } else {
            msg.arg1 = FLAG_SUCCESS
        }
        handler.sendMessage(msg)
    }

    private fun singleInventory() {
        mStrTime = System.currentTimeMillis()
        val info = mContext!!.uhf.inventorySingleTag()
        if (info != null) {
            val msg = handler.obtainMessage(FLAG_UHFINFO)
            msg.obj = info
            handler.sendMessage(msg)
        }
        handler.sendEmptyMessage(FLAG_UPDATE_TIME)
    }

    fun startInventory() {
        if (mContext!!.isScanning) {
            return
        }

        //--
        var time = etTime!!.text.toString()
        if (time.length > 0 && time.startsWith(".")) {
            etTime!!.setText("")
            time = ""
        }
        if (!time.isEmpty()) {
            maxRunTime = (time.toFloat() * 1000).toInt()
            clearData()
        } else {
            maxRunTime = etTime!!.hint.toString().toInt() * 1000
        }
        //--
        mContext!!.uhf.setInventoryCallback { uhftagInfo -> //                handler.sendMessage(handler.obtainMessage(FLAG_UHFINFO, uhftagInfo));
            handler.post { addTagToList(uhftagInfo) }
        }
        mContext!!.isScanning = true
        //        cbFilter.setChecked(true);
        val msg = handler.obtainMessage(FLAG_START)

        val inventoryParameter = InventoryParameter()
        inventoryParameter.setResultData(
            InventoryParameter.ResultData().setNeedPhase(cbPhase!!.isChecked)
        )
        if (mContext!!.uhf.startInventoryTag(inventoryParameter)) {
            mStrTime = System.currentTimeMillis()
            msg.arg1 = FLAG_SUCCESS
            handler.sendEmptyMessage(FLAG_UPDATE_TIME)
            handler.removeMessages(FLAG_TIME_OVER)
            handler.sendEmptyMessageDelayed(FLAG_TIME_OVER, maxRunTime.toLong())
        } else {
            msg.arg1 = FLAG_FAIL
            mContext!!.isScanning = false
        }
        handler.sendMessage(msg)
    }


    private fun addTagToList(uhftagInfo: UHFTAGInfo) {
        // Log.i(TAG, "addTagToList: " + uhftagInfo.getEPC() + " " + uhftagInfo.getTid() + " " + uhftagInfo.getUser() + " " + uhftagInfo.getReserved());
        if (uhftagInfo.reserved == null) uhftagInfo.reserved = ""
        if (uhftagInfo.epc == null) uhftagInfo.epc = ""
        if (uhftagInfo.tid == null) uhftagInfo.tid = ""
        if (uhftagInfo.user == null) uhftagInfo.user = ""

        for (tag in tagList) {
            if (tag.epc == uhftagInfo.epc) {
                // Update existing tag's RSSI and phase
                tag.rssi = uhftagInfo.rssi
                tag.phase = uhftagInfo.phase

                // Increment count if duplicates are shown
                if (mContext!!.isShowDuplicateTags) {
                    tag.count = tag.count + 1
                    tv_total!!.text = (++total).toString()
                }
                adapter!!.notifyDataSetChanged()
                return  // Exit the method, no need to add duplicate
            }
        }
        // Add new unique tag
        uhftagInfo.setExtraData(KEY_TAG, generateTagString(uhftagInfo))
        tagList.add(uhftagInfo)
        tv_count!!.text = tagList.size.toString()
        tv_total!!.text = (++total).toString()
        adapter!!.notifyDataSetChanged()

        */
/*boolean[] exists = new boolean[1];
        int index = CheckUtils.getInsertIndex(tagList, uhftagInfo, exists);
        if (exists[0]) {
            tagList.get(index).setRssi(uhftagInfo.getRssi());
            tagList.get(index).setPhase(uhftagInfo.getPhase());
            if (mContext.isShowDuplicateTags) {
                tagList.get(index).setCount(tagList.get(index).getCount() + 1);
                tv_total.setText(String.valueOf(++total));
            }
        } else {
            uhftagInfo.setExtraData(KEY_TAG, generateTagString(uhftagInfo));
            tagList.add(index, uhftagInfo);
            tv_count.setText(String.valueOf(tagList.size()));
            tv_total.setText(String.valueOf(++total));
        }
        adapter.notifyDataSetChanged();*//*

    }

    private fun generateTagString(uhftagInfo: UHFTAGInfo): String {
        Log.i(
            TAG,
            "reserved=" + uhftagInfo.reserved + ", epc=" + uhftagInfo.epc + ", tid=" + uhftagInfo.tid + ", user=" + uhftagInfo.user
        )
        var data = ""
        if (uhftagInfo.reserved != null && !uhftagInfo.reserved.isEmpty()) {
            data += "RESERVED:" + uhftagInfo.reserved
            data += """
                
                EPC:${uhftagInfo.epc}
                """.trimIndent()
        } else {
            data += if (TextUtils.isEmpty(uhftagInfo.tid)) uhftagInfo.epc else "EPC:" + uhftagInfo.epc
        }
        if (!TextUtils.isEmpty(uhftagInfo.tid) && (uhftagInfo.tid != "0000000000000000") && (uhftagInfo.tid != "000000000000000000000000")) {
            data += """
                
                TID:${uhftagInfo.tid}
                """.trimIndent()
        }
        if (uhftagInfo.user != null && uhftagInfo.user.length > 0) {
            data += """
                
                USER:${uhftagInfo.user}
                """.trimIndent()
        }
        return data
    }


    internal inner class ConnectStatus : IConnectStatus {
        override fun getStatus(connectionStatus: ConnectionStatus?) {
            Log.i(TAG, "getStatus connectionStatus=$connectionStatus")
            if (connectionStatus == ConnectionStatus.CONNECTED) {
                if (!mContext!!.isScanning) {
                    try {
                        Thread.sleep(500)
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                    }
                    btStartScan!!.isEnabled = true
                    setViewAlpha(btStartScan!!, 1f)
                    btnSingleInventory!!.isEnabled = true
                }

                cbFilter!!.isEnabled = true
            } else if (connectionStatus == ConnectionStatus.DISCONNECTED) {
                stop()
                btClear!!.isEnabled = true
                btStop!!.isEnabled = false
                setViewAlpha(btStop!!, 0.3f)
                btStartScan!!.isEnabled = false
                setViewAlpha(btStartScan!!, 0.3f)
                btnSingleInventory!!.isEnabled = false

                cbFilter!!.isChecked = false
                cbFilter!!.isEnabled = false
            }
        }
    }


    //-----------------------------
    private var selectIndex = -1

    inner class ViewHolder {
        var tvTag: TextView? = null
        var tvTagCount: TextView? = null
        var tvTagRssi: TextView? = null
        var tvPhase: TextView? = null

        var checkBox: CheckBox? = null
    }

    inner class MyAdapter(context: Context?) : BaseAdapter() {
        private val mInflater: LayoutInflater = LayoutInflater.from(context)
        private val mContext: Context? = null
        private val checkedItems =
            SparseBooleanArray() // To track checked state
        // Initialize storage

        override fun getCount(): Int {
            return tagList.size
        }

        override fun getItem(arg0: Int): Any {
            return tagList[arg0]
        }

        override fun getItemId(arg0: Int): Long {
            return arg0.toLong()
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            var convertView = convertView
            var holder: ViewHolder? = null
            if (convertView == null) {
                holder = ViewHolder()
                convertView = mInflater.inflate(R.layout.listtag_items, null)
                holder!!.tvTag = convertView.findViewById(R.id.TvTag)
                holder.tvTagCount = convertView.findViewById(R.id.TvTagCount)
                holder.tvTagRssi = convertView.findViewById(R.id.TvTagRssi)
                holder.tvPhase = convertView.findViewById(R.id.TvPhase)
                holder.checkBox =
                    convertView.findViewById(R.id.customCheckBox) // Assuming you have a checkbox

                convertView.tag = holder
            } else {
                holder = convertView.tag as ViewHolder
            }

            holder!!.tvTag!!.text = tagList[position].getExtraData(KEY_TAG)
            holder.tvTagCount!!.text = tagList[position].count.toString()
            holder.tvTagRssi!!.text = tagList[position].rssi
            holder.tvPhase!!.text = tagList[position].phase.toString()

            // Handle checkbox state
            holder.checkBox!!.setOnCheckedChangeListener(null) // Avoid triggering listener on recycling
            holder.checkBox!!.isChecked = checkedItems[position, false]

            // Handle checkbox click
            holder.checkBox!!.setOnCheckedChangeListener { buttonView: CompoundButton?, isChecked: Boolean ->
                checkedItems.put(position, isChecked)
            }

            if (position == selectIndex) {
                convertView!!.setBackgroundColor(mContext!!.resources.getColor(R.color.lightblue3))
            } else {
                convertView!!.setBackgroundColor(Color.TRANSPARENT)
            }
            return convertView
        }
    }

    companion object {
        var KEY_TAG: String = "KEY_TAG"
    }
}
*/
