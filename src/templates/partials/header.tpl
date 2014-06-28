<!-- if has title, use the title -->
{{#title}}<title>{{title}}</title>{{/title}}

<!-- No title, use default one -->
{{^title}}<title>Jarvis</title>{{/title}}
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<link href="/css/lib/bootstrap.min.css" rel="stylesheet"/>
<link href="/css/lib/bootstrap-theme.min.css" rel="stylesheet"/>
<link href="/css/lib/font-awesome.min.css" rel="stylesheet"/>
<link href="/css/main.css" rel="stylesheet"/>
{{#dev?}}
  <!-- dev specific code -->
{{/dev?}}
{{#prod?}}
  <!-- productoion specific code -->
{{/prod?}}
