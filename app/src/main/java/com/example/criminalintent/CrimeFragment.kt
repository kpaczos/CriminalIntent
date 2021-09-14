package com.example.criminalintent

import android.app.Activity
import android.app.ApplicationErrorReport
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.text.format.DateFormat.format
import android.util.Log
import android.widget.*
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.fragment_crime.*
import java.lang.String.format
import android.text.format.DateFormat
import android.view.*
import androidx.core.content.FileProvider
import java.io.File
import java.text.MessageFormat.format
import java.util.*
private const val TAG = "CrimeFragment"
private const val ARG_CRIME_ID ="crime_id"
private const val DIALOG_DATE="DialogDate"
private const val REQUEST_DATE=0
private const val DATE_FORMAT="EEE,MMM,dd"
private const val REQUEST_CONTACT=1
private const val DIALOG_TIME = "DialogTime"
private const val REQUEST_CODE = 0
private const val REQUEST_PHOTO=2
class CrimeFragment:Fragment(),DatePickerFragment.Callbacks,TimePickerFragment.Callbacks {

    private lateinit var titleField: EditText
    private lateinit var dateButton: Button
    private lateinit var timeButton: Button
    private lateinit var solvedCheckBox: CheckBox
    private lateinit var reportButton: Button
    private lateinit var suspectButton: Button
    private lateinit var photoButton: ImageButton
    private lateinit var photoView: ImageView
    private lateinit var photoFile:File
    private lateinit var photoUri:Uri


    private val crimeDetailViewModel:CrimeDetailViewModel by lazy {
        ViewModelProviders.of(this).get(CrimeDetailViewModel::class.java)
    }

    private lateinit var crime:Crime
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        crime=Crime()
        val crimeId:UUID=arguments?.getSerializable(ARG_CRIME_ID)as UUID
        crimeDetailViewModel.loadCrime(crimeId)

    }
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_crime, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.delete_crime -> {
                crimeDetailViewModel.deleteCrime(crime)
                activity?.onBackPressed()
                true
            }
            else -> return false
        }
    }


    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_crime, container, false)
        titleField = view.findViewById(R.id.crime_title) as EditText
        dateButton = view.findViewById(R.id.crime_date) as Button
        timeButton = view.findViewById(R.id.crime_time) as Button
        solvedCheckBox = view.findViewById(R.id.crime_solved) as CheckBox
        reportButton=view.findViewById(R.id.crime_report) as Button
        suspectButton=view.findViewById(R.id.crime_suspect) as Button
        photoButton=view.findViewById(R.id.crime_camera) as ImageButton
        photoView = view.findViewById(R.id.crime_photo) as ImageView
        return view

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        crimeDetailViewModel.crimeLiveData.observe(
                viewLifecycleOwner,
                androidx.lifecycle.Observer { crime ->
                    crime?.let {
                        this.crime=crime
                        photoFile=crimeDetailViewModel.getPhotoFile(crime)
                       photoUri=FileProvider.getUriForFile(requireActivity(),"com.example.criminalintent.fileprovider",photoFile)
                        updateUI()
                    }
                }
        )
    }
    override fun onStart() {
        super.onStart()

        val titleWatcher = object : TextWatcher {

            override fun beforeTextChanged(
                    sequence: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
            ) {
                // This space intentionally left blank
            }

            override fun onTextChanged(
                    sequence: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int
            ) {
                crime.title = sequence.toString()
            }

            override fun afterTextChanged(sequence: Editable?) {
                // This one too
            }
        }
        titleField.addTextChangedListener(titleWatcher)

        solvedCheckBox.apply {
            setOnCheckedChangeListener{_,isChecked->
                crime.isSolved=isChecked
            }
        }
        /*dateButton.setOnClickListener {
            DatePickerFragment.newInstance(crime.date).apply {
                setTargetFragment(this@CrimeFragment, REQUEST_DATE)
                show(this@CrimeFragment.requireFragmentManager(), DIALOG_DATE)
            }
        }

        crime_time.setOnClickListener {
            val calendar = Calendar.getInstance()
            val timeListener = TimePickerDialog.OnTimeSetListener{ timePicker: TimePicker, hourOfDay: Int, minute: Int ->
                calendar.set(Calendar.HOUR_OF_DAY,hourOfDay)
                calendar.set(Calendar.MINUTE,minute)
                crime_time.text=java.text.SimpleDateFormat("HH:mm").format(calendar.time)

            }
            TimePickerDialog(context,timeListener,calendar.get(Calendar.HOUR_OF_DAY),calendar.get(Calendar.MINUTE),false).show()
            updateUI()
        }*/
        dateButton.setOnClickListener {
            openDialogFragment(DatePickerFragment.newInstance(crime.date), DIALOG_DATE)
        }


        timeButton.setOnClickListener {
            openDialogFragment(TimePickerFragment.newInstance(crime.date), DIALOG_TIME)

        }
        reportButton.setOnClickListener {
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, getCrimeReport())
                putExtra(
                        Intent.EXTRA_SUBJECT,
                        getString(R.string.crime_report_subject))
            }.also { intent ->
                val chooserIntent = Intent.createChooser(intent,getString(R.string.send_report))
                startActivity(chooserIntent)
            }
        }
        suspectButton.apply {
            val pickContactIntent=Intent(Intent.ACTION_PICK,ContactsContract.Contacts.CONTENT_URI)

            setOnClickListener {
                startActivityForResult(pickContactIntent, REQUEST_CONTACT)
            }

            val packageManager:PackageManager=requireActivity().packageManager
            val resolvedActivity:ResolveInfo?=packageManager.resolveActivity(pickContactIntent,PackageManager.MATCH_DEFAULT_ONLY)
            if(resolvedActivity==null){
                isEnabled=false
            }
        }
      photoButton.apply {
            val packageManager:PackageManager=requireActivity().packageManager

            val captureImage=Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            val resolvedActivity:ResolveInfo?=packageManager.resolveActivity(captureImage,PackageManager.MATCH_DEFAULT_ONLY)
            if (resolvedActivity==null)
            {
                isEnabled=false
            }
            setOnClickListener {
                captureImage.putExtra(MediaStore.EXTRA_OUTPUT,photoUri)

                val cameraActivities:List<ResolveInfo> = packageManager.queryIntentActivities(captureImage,PackageManager.MATCH_DEFAULT_ONLY)

                for (cameraActivity in cameraActivities){
                    requireActivity().grantUriPermission(cameraActivity.activityInfo.packageName,photoUri,Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                }
                startActivityForResult(captureImage, REQUEST_PHOTO)
            }
        }

    }


    override fun onStop() {
        super.onStop()
        crimeDetailViewModel.saveCrime(crime)
    }

   override fun onDetach() {
        super.onDetach()
        requireActivity().revokeUriPermission(photoUri,
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
    }

    override fun onDateSelected(date: Date) {
        crime.date=date
        updateUI()
    }

    override fun onTimeSelected(date: Date) {
        crime.date=date
        updateUI()
    }
    private fun updateUI(){
        titleField.setText(crime.title)
        dateButton.text=DateFormat.format("EEE dd MMM yyyy",crime.date)
        timeButton.text=DateFormat.format("HH:mm",crime.date)
        solvedCheckBox.apply {
            isChecked = crime.isSolved
            jumpDrawablesToCurrentState()
        }
        if(crime.suspect.isNotEmpty()){
            suspectButton.text=crime.suspect
        }
        updatePhotoView()
    }

   private fun updatePhotoView(){
        if(photoFile.exists()){
            val bitmap = getScaledBitmap(photoFile.path,requireActivity())
            photoView.setImageBitmap(bitmap)
        }
        else{
            photoView.setImageBitmap(null)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when{
            resultCode!=Activity.RESULT_OK->return

            requestCode== REQUEST_CONTACT && data!=null->{
                val contactUri:Uri?=data.data
                val queryFields= arrayOf(ContactsContract.Contacts.DISPLAY_NAME)
                val cursor = requireActivity().contentResolver
                        .query(contactUri!!,queryFields,null,null,null)
                cursor?.use {
                    if (it.count==0){
                        return
                    }

                    it.moveToFirst()
                    val suspect = it.getString(0)
                    crime.suspect=suspect
                    crimeDetailViewModel.saveCrime(crime)
                    suspectButton.text=suspect
                }
            }
            requestCode== REQUEST_PHOTO->{
                requireActivity().revokeUriPermission(photoUri,Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                updatePhotoView()

            }
        }
    }
    private fun getCrimeReport(): String {
        val solvedString = if (crime.isSolved) {
            getString(R.string.crime_report_solved)
        } else {
            getString(R.string.crime_report_unsolved)
        }

        val dateString = android.text.format.DateFormat.format(DATE_FORMAT, crime.date)
        var suspect = if (crime.suspect.isBlank()) {
            getString(R.string.crime_report_no_suspect)
        } else {
            getString(R.string.crime_report_suspect, crime.suspect)
        }

        return getString(R.string.crime_report,
                crime.title, dateString, solvedString, suspect)

    }

    private val openDialogFragment: (DialogFragment, String) -> Unit = { dialogFragment, dialogArg ->
        dialogFragment.apply {
            setTargetFragment(this@CrimeFragment, REQUEST_CODE)
            show(this@CrimeFragment.requireFragmentManager(), dialogArg)
        }
    }

    companion object{
        fun newInstance(crimeId:UUID):CrimeFragment{
            val args = Bundle().apply {
                putSerializable(ARG_CRIME_ID,crimeId)
            }
            return CrimeFragment().apply {
                arguments=args
            }
        }
    }
}