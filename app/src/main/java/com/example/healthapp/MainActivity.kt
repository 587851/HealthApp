package com.example.healthapp

import android.health.connect.datatypes.Record
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    var items = arrayOf("Steps", "Weigth", "Calories burned");
    var choosen = "none";
    private val healthConnectManager by lazy { HealthConnectManager(this) }
    private val hapi = hapifhir()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_layout)

        //thread {
          //  hapi.doSomething();
        //}
        //h.createPatient();

        val autoComplete : AutoCompleteTextView = findViewById(R.id.autocomplete);
        val adapter = ArrayAdapter(this, R.layout.list_item, items);
        autoComplete.setAdapter(adapter);
        autoComplete.setSelection(0);
        //
        // Add an OnItemClickListener to respond to item selections
        autoComplete.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
            val selectedText = parent.getItemAtPosition(position).toString()
            choosen = selectedText
            updateTextBox(selectedText)
        }

        val button : Button = findViewById(R.id.button)
        button.setOnClickListener{
            val editTextValue : EditText = findViewById(R.id.plain_text_input);
            //var editTextStartTime : EditText = findViewById(R.id.startTimeInput)
            //var editTextFinishTime : EditText =  findViewById(R.id.finishTimeInput);
            val textValue = editTextValue.text.toString()

            //val start = toZonedDateTime(editTextStartTime.text.toString())
            //val finish = toZonedDateTime(editTextFinishTime.text.toString())
            val now = ZonedDateTime.now();
            Thread.sleep(1000);
            val now1 = ZonedDateTime.now();

            if(!choosen.equals("none") && textValue.isNotEmpty()){
                val value = textValue.toDouble();
                lifecycleScope.launch {
                    healthConnectManager.writeData(choosen, value, now, now1)
                    updateTextBox(choosen)
            }

            }
        }

        val buttonSend : Button = findViewById(R.id.buttonSend)
        buttonSend.setOnClickListener{

            val given : String = findViewById<EditText>(R.id.givenNameText).text.toString();
            val family : String = findViewById<EditText>(R.id.familyNameText).text.toString();

            System.out.println(given + "   " + given.isEmpty())

            if(given.isEmpty() || family.isEmpty() || choosen.equals("none")){
                findViewById<TextView>(R.id.outputTextView).setText("Given, familiy and type can not be empty")
            }else{
                thread{
                    var records : List<androidx.health.connect.client.records.Record> = emptyList()
                    lifecycleScope.launch {
                        records =
                            healthConnectManager.readDataFromLastWeekToRecord(choosen)
                    }
                        val p = hapi.addPatient(given, family)

                        for (record in records) {
                            when (record) {
                                is StepsRecord -> hapi.addObservation("Steps", record.count.toDouble(), p, record.startTime, record.endTime)
                                is WeightRecord -> hapi.addObservation("Weight", record.weight.inKilograms, p, record.time, null)
                                is TotalCaloriesBurnedRecord -> hapi.addObservation("Calories burned", record.energy.inKilocalories, p, record.startTime, record.endTime)
                            }
                        }
                    }

                }
            }
        }


    fun updateTextBox(selectedText : String) {
        lifecycleScope.launch {
            val data: String = healthConnectManager.readDataFromLastWeekToString(selectedText)
            val outputBox: TextView = findViewById(R.id.outputTextView)
            outputBox.text = data
        }
    }


    fun toZonedDateTime(inputText : String): ZonedDateTime {
        var returnValue : ZonedDateTime = ZonedDateTime.now();
        try {
            // Define a DateTimeFormatter for parsing the input
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd\'T\'HH:mm:ssXXX")

            // Parse the input text into a ZonedDateTime
            returnValue = ZonedDateTime.parse(inputText, formatter)

            // Now, you have a ZonedDateTime object
            // You can use zonedDateTime for further processing
            // For example, you can display it or manipulate it
        } catch (e: DateTimeParseException) {

        }

        return returnValue;
    }

}