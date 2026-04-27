### Registration Flow API


Base path /registrationflow

## Controller api

GET /

{
    {
    "registrationflow_id":"<UUID>",
    "name":"Swedenconnect Sandbox Register",
    "description":"Swedenconnect registration flow",
    "flowSteps":{
        "id":""
        "name":"SwedenConnect RP MetadataValidation"
    }

}



## Database Table layout

registrationflow_id UUID,
taImId UUID
name varchar 255,
description varchar 255",
flowSteps smalltext


## ClassImplementation

Step