# Winstone Servlet Filter #

## GzipFilter ##

A filter that checks if the request will accept a gzip encoded response, and if so wraps the response in a gzip encoding response wrapper.

class name : net.winstone.filters.gzip.GzipFilter


## MultipartRequestFilter ##

Checks the content type, and wraps the request in a MultipartRequestWrapper  if it's a multipart request.

class name: net.winstone.filters.multipart.MultipartRequestFilter

Parameters:
  * axContentLength: maximum lenght (-1 : no limit)
  * ooBigPage: error page uri to redirect if content is too big