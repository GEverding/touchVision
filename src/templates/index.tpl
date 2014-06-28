<!DOCTYPE html>
<html>
  <!-- http://mustache.github.com/mustache.5.html -->
  <head>
    <!-- partial is just like copy and paste the template here -->
    {{>partials/header}}
  </head>
  <body>
    <div id="content" class="js-app"></div>

    <script src="/js/lib/react.js"></script>
    <script src="/js/lib/jquery.min.js"></script>
    <script src="/js/lib/lodash.min.js"></script>
    <script src="/js/lib/d3.min.js"></script>
    <script src="/js/lib/bootstrap.min.js"></script>
    <script src="/js/out/goog/base.js" type="text/javascript"></script>
    <script src="/js/touchVision.js" type="text/javascript"></script>
    <script type="text/javascript">goog.require("client.core");</script>
  </body>
</html>
