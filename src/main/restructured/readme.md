# Documentation is in ReStructuredText format

Couple of useful links:
* https://docutils.sourceforge.io/docs/ref/rst/restructuredtext.html
* https://www.sphinx-doc.org/en/master/usage/restructuredtext/basics.html

Headings priority/order:

* `========` - for 1
* `--------` - for 1.1
* `^^^^^^^^` - for 1.1.1
* `~~~~~~~~` - for 1.1.1.1 (to keep consistent you previous documents, it will only show highlight title without 1.1.1.1)
* `''''''''` - for 1.1.1.1.1 (to keep consistent you previous documents, it will only show highlight title without 1.1.1.1.1)

# Configuration of ReadTheDocs and legacy documentation

Previous documentation in AsciiDoc was moved from https://docs.tigase.net to https://docs-legacy.tigase.net ([CloudFront configuration: `E1OF45YJ7KFOUO` / d2jb0rmum93y88.cloudfront.net](https://us-east-1.console.aws.amazon.com/cloudfront/v3/home?region=us-east-1&skipRegion=true#/distributions/E1OF45YJ7KFOUO)).

In order to maintain documentation in working state [redirections were configured in ReadTheDocs](https://readthedocs.com/dashboard/tigase-tigase-server/redirects/) for all available projects

Each time documentation is converted and migrated from AsciiDoc to ReadTheDocs it **MUST** be included in the [`index.rst`](./index.rst) file!