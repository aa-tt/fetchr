package com.pharmeasy.fetchr.activity


import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import com.pharmeasy.fetchr.R
import com.pharmeasy.fetchr.greendao.TaskRepoNew
import com.pharmeasy.fetchr.retro.eventOf
import com.pharmeasy.fetchr.service.SessionService
import com.pharmeasy.fetchr.service.TicketService
import com.pharmeasy.fetchr.type.UserAction
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.create_ticket_content.*
import android.provider.MediaStore
import android.widget.Toast
import okhttp3.MultipartBody
import okhttp3.RequestBody
import android.database.Cursor
import android.net.Uri
import android.support.v7.widget.Toolbar
import android.util.Log
import okhttp3.MediaType
import java.io.File
import com.pharmeasy.fetchr.constants.SCREENSHOT_TYPE
import com.pharmeasy.fetchr.main.ui.MainActivity
import com.pharmeasy.fetchr.retro.retroWithToken
import kotlinx.android.synthetic.main.create_ticket_main.*


class CreateTicket : BaseActivity(), AdapterView.OnItemSelectedListener {

    private var file : File? = null

    override val rootView: View?
        get() = root_panel
    override val progressIndicator: ProgressBar?
        get() = null

    private val raiseTicketIssue by lazy {
       retroWithToken(TicketService::class.java)
    }

    private val repo by lazy {
        TaskRepoNew(context = this)
    }

    private val GALLERY_INTENT : Int = 10;

    private var priorities = arrayOf("Select Severity of Issue", "Urgent", "High", "Medium", "Low")

    private var spinner: Spinner? = null

    private var priority: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.create_ticket_main)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        toolbar.setNavigationOnClickListener {
            finish()
        }

        spinner = this.priority_spinner
        spinner!!.onItemSelectedListener = this

        // Create an ArrayAdapter using a simple spinner layout and priorities array
        val arrayAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, priorities)

        // Set layout to use when the list of choices appear
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        // Set Adapter to Spinner
        spinner!!.adapter = arrayAdapter

        button_save_issue?.setOnClickListener {
            saveIssue()
        }

        button_add_screenshot.setOnClickListener {
            val galleryIntent = Intent(Intent.ACTION_PICK,
                    android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(galleryIntent, GALLERY_INTENT)
        }
    }


    override fun onItemSelected(arg0: AdapterView<*>, arg1: View, position: Int, id: Long) {
        //if (position != 0)
       priority = position

    }

    override fun onNothingSelected(arg0: AdapterView<*>) {
    }

    //SAVE ISSUE BUTTON TASK

    private fun valid(): Boolean {
        return (edit_title_ticket.text.trim().isNotEmpty() && edit_description_ticket.text.trim().isNotEmpty() && edit_cc_ticket.text.trim().isNotEmpty() && text_add_screenshot.text.trim().isNotEmpty())
    }

    fun saveIssue() {

        if (!valid()) {
            error("Please fill all the fields")
            return
        }

        showProgress()

        val fileBody = createMultipartFormData(file!!)

        //TODO replace taskId, shipmentId, tripId with original data
        val title = RequestBody.create(
                MultipartBody.FORM,edit_title_ticket.text.trim().toString() )
        val description = RequestBody.create(
                MultipartBody.FORM, edit_description_ticket.text.trim().toString())
        val priority = RequestBody.create(
                MultipartBody.FORM, priority!!.toString())
        val cc = RequestBody.create(
                MultipartBody.FORM, edit_cc_ticket.text.trim().toString())

        raiseTicketIssue.raiseTicket(fileBody,title,description,priority, cc)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { _ -> process() },
                        { error -> processError(error) }
                )
    }

    private fun process() {

        Toast.makeText(this, "Ticket raised successfully.", Toast.LENGTH_SHORT).show()

        repo.addEvent(eventOf(UserAction.RAISE_TICKET, SessionService.userId))
        //repo.clearTasksForUser(SessionService.userId)

        launchMain()
    }

    private fun launchMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }


    private fun createMultipartFormData(signatureFile: File): MultipartBody.Part {
        // create RequestBody instance from file

        val file = File(signatureFile.toString())
        val requestFile = RequestBody.create(
                MediaType.parse(SCREENSHOT_TYPE),
                file
        )
        Log.d("CreateTicket", "$file")
        return MultipartBody.Part.createFormData("screenshot", file.name, requestFile)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode){
            GALLERY_INTENT -> {
                if (data != null) {
                    val uri: Uri = data.data
                    val path: String? = getPathFromUri(uri)

                    file = File(path)


                    val fileName: String
                    if (uri.scheme == "file") {
                        fileName = uri.lastPathSegment
                    } else {
                        var cursor: Cursor? = null
                        try {
                            cursor = contentResolver.query(uri, arrayOf(MediaStore.Images.ImageColumns.DISPLAY_NAME), null, null, null)

                            if (cursor != null && cursor.moveToFirst()) {
                                fileName = cursor.getString(cursor.getColumnIndex(MediaStore.Images.ImageColumns.DISPLAY_NAME))
                                Log.d("file", "name is $fileName")

                                text_add_screenshot.text = fileName
                            }
                        } finally {

                            cursor?.close()
                        }
                    }
                }


            }
        }
    }

    private fun getPathFromUri(contentUri : Uri) : String? {
        var res : String? = null
        var proj : Array<String> = arrayOf(MediaStore.Images.Media.DATA)
        val cursor :Cursor = getContentResolver().query(contentUri, proj, null, null, null);
        if (cursor.moveToFirst()){
            val column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            res = cursor.getString(column_index)
        }
        cursor.close()
        return  res

    }
}




