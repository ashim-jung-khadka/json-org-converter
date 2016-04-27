# json-org-converter
Server Side Converter for Json.org

We choose Restful API not just for platform independent. We choose, to make multiple platform communication easier. To do this, we have to make some standard  so that everyone knows what they are getting and giving. But the problem here is, API standard should me made based on some agreement between multiple platform and it takes lot more time and resources. 

To ease this, jsonapi.org had made some standard, so that rest of us can take a good sleep at night. But it haven't provided tools. Many other companies and parties have provided it, and make us dependent to their. And that where my converter comes in. It allows us to independently map between object and json, and rest of is yours what to do with the output.

Implementation is very simple, what you do here is:
  Input : Object
  Output : json -- which is based upon jsonapi.org
  
  Input : json
  Output : object -- which is your pojo object
