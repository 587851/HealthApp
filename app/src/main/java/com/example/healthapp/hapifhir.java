package com.example.healthapp;

import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Quantity;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Type;

import java.time.Instant;
import java.util.Date;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.StringClientParam;

public class hapifhir {

    private FhirContext ctxR4;
    private String serverBase = "https://hapi.fhir.org/baseR4";

    public hapifhir(){
        ctxR4 = FhirContext.forR4();
    }

    public Patient addPatient(String given, String family){

        Patient p = new Patient();
        p.addName().setFamily(family).addGiven(given);

        IGenericClient client = ctxR4.newRestfulGenericClient(serverBase);
        MethodOutcome mo = client.create().resource(p).prettyPrint().encodedJson().execute();
        IIdType id = mo.getId();
        System.out.println("Got Patient ID: " + id.getValue());

        return p;
    }

    public void addObservation(String datatype, Double value, Patient patient, Instant start, Instant end){

        Observation observation= new Observation();
        observation.setStatus(Observation.ObservationStatus.FINAL);
        observation.setSubject(new Reference(patient.getIdElement().getValue()));


        switch (datatype){
            case "Weight":
                observation.setValue(new Quantity()
                        .setValue(value)
                        .setSystem("http://unitsofmeasure.org")
                        .setCode("kg"));
                observation
                        .getCode()
                        .addCoding()
                        .setSystem("http://loinc.org")
                        .setCode("29463-7")
                        .setDisplay("Body Weight");
                observation.getEffectiveInstantType().setValue(Date.from(start));
                break;
            case "Steps":
                observation.setValue(new Quantity()
                        .setValue(value)
                        .setSystem("http://unitsofmeasure.org")
                        .setCode("steps"));
                observation
                        .getCode()
                        .addCoding()
                        .setSystem("http://loinc.org")
                        .setCode("66334-4")
                        .setDisplay("Steps taken");
                observation.getEffectivePeriod()
                        .setStart(Date.from(start))
                        .setEnd(Date.from(end));
                break;
            case "Calories burned":
                observation.setValue(new Quantity()
                        .setValue(value)
                        .setSystem("http://unitsofmeasure.org")
                        .setCode("kcal"));
                observation
                        .getCode()
                        .addCoding()
                        .setSystem("http://loinc.org")
                        .setCode("41981-2")
                        .setDisplay("Calories burned");
                observation.getEffectivePeriod()
                        .setStart(Date.from(start))
                        .setEnd(Date.from(end));
                break;
        }

        IGenericClient client = ctxR4.newRestfulGenericClient(serverBase);
        MethodOutcome mo = client.create().resource(observation).prettyPrint().encodedJson().execute();
        IIdType id = mo.getId();
        System.out.println("Got Observation ID: " + id.getValue());
    }



    public void createPatient(String fornavn, String etternavn, String land, String by, String addresse, String postkode){

        IGenericClient client = ctxR4.newRestfulGenericClient(serverBase);

        Bundle results = client.search()
                .forResource(Patient.class)
                .where(Patient.FAMILY.matches().value("james"))
                .returnBundle(Bundle.class)
                .execute();

        System.out.println("Found " + results.getEntry().size() + " patients named 'duck'");

        IParser parser = ctxR4.newJsonParser();
        Patient patient = new Patient();
        patient.addName()
                .setUse(HumanName.NameUse.OFFICIAL)
                .setFamily(etternavn)
                .addGiven(fornavn);
        patient.addAddress()
                .setCity(by)
                .setPostalCode(postkode)
                .setUse(Address.AddressUse.HOME)
                .setCountry(land)
                .setText(addresse);
    }

    public void doSomething(){
        IParser parser = ctxR4.newJsonParser();
        Patient patient = new Patient();



        patient.addIdentifier().setSystem("http://example.com/fictitious-mrns").setValue("MRN001");
        patient.setGender(Enumerations.AdministrativeGender.MALE);
        patient.addName()
                .setUse(HumanName.NameUse.OFFICIAL)
                .setFamily("Tester")
                .addGiven("John")
                .addGiven("Q");

        parser.setPrettyPrint(true);
        String encoded = parser.encodeResourceToString(patient);
        System.out.println(encoded);


        String patientId = patient.getIdentifier().get(0).getValue();
        String familyName = patient.getName().get(0).getFamily().toString();
        Enumerations.AdministrativeGender gender = patient.getGender();

        System.out.println(patientId); // PRP1660
        System.out.println(familyName); // Cardinal
        System.out.println(gender); // male;

        IGenericClient client = ctxR4.newRestfulGenericClient(serverBase);

        Bundle bundle = (Bundle) client.search()
                .forResource(Patient.class)
                .where(Patient.FAMILY.matches().value("Torje"))
                .returnBundle(Bundle.class)
                .execute();

        System.out.println("Found " + bundle.getEntry().size() + " patients named 'duck'");

    }

}
