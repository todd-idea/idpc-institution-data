package org.ideaedu

import groovyx.net.http.RESTClient
import groovyx.net.http.ContentType

/**
 * The Main class provides a way to pull IDEA Feedback System data from the
 * IDEA Data Portal. In this case, it pulls institution data and
 * dumps the requested data points (institution information and their
 * associated usage stats) to a CSV file. It has some optional command
 * line arguments that control the behavior. The arguments include:
 * <ul>
 * <li>h (host) - the hostname of the IDEA Data Portal</li>
 * <li>p (port) - the port to communicate to the IDEA Data Portal on</li>
 * <li>v (verbose) - provide more output on the command line</li>
 * <li>a (app) - the client application name</li>
 * <li>k (key) - the client application key</li>
 * <li>? (help) - show the usage of this</li>
 * </ul>
 *
 * @author Todd Wallentine todd AT IDEAedu org
 */
public class Main {

    private static final def DEFAULT_HOSTNAME = "rest.ideasystem.org"
    private static final def DEFAULT_PORT = 443
    private static final def DEFAULT_BASE_PATH = "IDEA-REST-SERVER/v1"
    private static final def DEFAULT_AUTH_HEADERS = [ "X-IDEA-APPNAME": "", "X-IDEA-KEY": "" ]
    private static final def DEFAULT_PROTOCOL = "https"

    /** The maximum number of surveys to get before quitting. */
    private static final def MAX_SURVEYS = 50000

    /** The number of surveys to get per page */
    private static final def PAGE_SIZE = 100

    private static def hostname = DEFAULT_HOSTNAME
    private static def protocol = DEFAULT_PROTOCOL
    private static def port = DEFAULT_PORT
    private static def basePath = DEFAULT_BASE_PATH
    private static def authHeaders = DEFAULT_AUTH_HEADERS

    private static def verboseOutput = false

    private static RESTClient restClient

    public static void main(String[] args) {

        def cli = new CliBuilder( usage: 'Main -v -h host -p port -a "TestClient" -k "ABCDEFG123456"' )
        cli.with {
            v longOpt: 'verbose', 'verbose output'
            h longOpt: 'host', 'host name (default: rest.ideasystem.org)', args:1
            p longOpt: 'port', 'port number (default: 443)', args:1
            a longOpt: 'app', 'client application name', args:1
            k longOpt: 'key', 'client application key', args:1
            '?' longOpt: 'help', 'help'
        }
        def options = cli.parse(args)
        if(options.'?') {
            cli.usage()
            return
        }
        if(options.v) {
            verboseOutput = true
        }
        if(options.h) {
            hostname = options.h
        }
        if(options.p) {
            port = options.p.toInteger()
        }
        if(options.a) {
            authHeaders['X-IDEA-APPNAME'] = options.a
        }
        if(options.k) {
            authHeaders['X-IDEA-KEY'] = options.k
        }



        /*
         * The following will get all the surveys that are available of the
         * given type and print out the overall ratings for each survey subject.
         * This will print the raw and adjusted mean and t-score for each survey
         * subject.
         */
        def institutionMap = [:]
        def surveys = getAllSurveys()
        if(surveys) {
            surveys.each { survey ->
                def institutionID = survey.institution_id
                if(!institutionMap[institutionID]) {
                    institutionMap[institutionID] = createInitialMap()
                }

                def raterFormID = survey.rater_form.id
                institutionMap[institutionID][raterFormID] = institutionMap[institutionID][raterFormID] + 1
            }

            // Now print the collected data

            // Print the CSV header
            println"ID,Name,FICE,Chair,Admin,Teaching,Learning,Diagnostic"
            institutionMap.each { institutionID, instrumentCounts ->
                def institution = getInstitution(institutionID)
                print "${institution.id},${institution.name},${institution.fice},"
                print "${instrumentCounts[14]}," // Chair (14)
                print "${instrumentCounts[18]}," // Admin (18)
                print "${instrumentCounts[20]}," // Teaching (20)
                print "${instrumentCounts[10]}," // Learning/Short (10)
                println "${instrumentCounts[9]}" // Diagnostic (9)
            }

        } else {
            println "No surveys are available."
        }
    }

    /**
     * Create an initial map of instrument type counts. Each response form ID is a key with an initial value of 0.
     */
    static def createInitialMap() {
        def map = [
            6: 0, // Admin 2.1 response form
            7: 0, // Chair 2.1 response form
            8: 0, // Dean 2.1 response form
            9: 0, // Diagnostic 2.1 response form
            10: 0, // Short/Learning Outcomes 2.1 response form
            14: 0, // Chair 2.6 response form
            18: 0, // Admin 3.0 response form
            20: 0 // Teaching Essentials 2014 response form
        ]

        return map
    }

    /**
     * Get all the surveys for the given type (chair, admin, diagnostic, short).
     *
     * @return A list of surveys of the given type; might be empty but never null.
     */
    static def getAllSurveys() {
        def surveys = []

        def client = getRESTClient()
        def resultsSeen = 0
        def totalResults = Integer.MAX_VALUE
        def currentResults = 0
        def page = 0
        while((totalResults > resultsSeen + currentResults) && (resultsSeen < MAX_SURVEYS)) {
            def response = client.get(
                path: "${basePath}/surveys",
                query: [ max: PAGE_SIZE, page: page ],
                requestContentType: ContentType.JSON,
                headers: authHeaders)
            if(response.status == 200) {
                if(verboseOutput) {
                    println "Surveys data: ${response.data}"
                }

                response.data.data.each { survey ->
                    surveys << survey
                }

                totalResults = response.data.total_results
                currentResults = response.data.data.size()
                resultsSeen += currentResults
                page++
            } else {
                println "An error occured while getting the surveys: ${response.status}"
                break
            }
        }

        return surveys
    }

    /**
     * Get the institution information that has the given ID.
     *
     * @param institutionID The ID of the institution.
     * @return The institution with the given ID.
     */
    static def getInstitution(institutionID) {
        def institution

        def client = getRESTClient()
        def response = client.get(
            path: "${basePath}/institutions",
            query: [ id: institutionID ],
            requestContentType: ContentType.JSON,
            headers: authHeaders)
        if(response.status == 200) {
            if(verboseOutput) {
                println "Institution data: ${response.data}"
            }

            if(response.data && response.data.data && response.data.data.size() > 0) {
                // take the first one ... not sure why we would end up with more than 1
                institution = response.data.data.get(0)
            }
        } else {
            println "An error occured while getting the institution with ID ${institutionID}: ${response.status}"
        }

        return institution
    }

    /**
     * Get an instance of the RESTClient that can be used to access the REST API.
     *
     * @return RESTClient An instance that can be used to access the REST API.
     */
    private static RESTClient getRESTClient() {
        if(restClient == null) {
            if(verboseOutput) println "REST requests will be sent to ${hostname} on port ${port} with protocol ${protocol}"

            restClient = new RESTClient("${protocol}://${hostname}:${port}/")
            restClient.ignoreSSLIssues()
            restClient.handler.failure = { response ->
                if(verboseOutput) {
                    println "The REST call failed with status ${response.status}"
                }
                return response
            }
        }

        return restClient
    }
}