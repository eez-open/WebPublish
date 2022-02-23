package com.envox.webpublish.Helper;

import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.Stack;

/**
 * <p>
 * Class that provides a method for doing regular expression string replacement by passing the matched string to
 * a function that operates on the string.  The result of the operation is then used to replace the original match.
 * </p>
 * <p>Example:</p>
 * <pre>
 * ReplaceCallback.find("string to search on", "/regular(expression/", new ReplaceCallback.Callback() {
 *      public String matches(MatchResult match) {
 *              // query db or whatever...
 *              return match.group().replaceAll("2nd level replacement", "blah blah");
 *      }
 * });
 * </pre>
 * <p>
 * This, in effect, allows for a second level of string regex processing.
 * </p>
 *
 */
public class ReplaceCallback {

    public static interface Callback {

        public String matches(MatchResult match);
    }
    private final Pattern pattern;
    private Callback callback;

    private class Result {

        int start;
        int end;
        String replace;
    }

    /**
     * You probably don't need this.  {@see find(String, String, Callback)}
     * @param regex             The string regex to use
     * @param callback  An instance of Callback to execute on matches
     */
    public ReplaceCallback(String regex, int flags, final Callback callback) {
        this.pattern = Pattern.compile(regex, flags);
        this.callback = callback;
    }

    public String execute(String string) {
        final Matcher matcher = this.pattern.matcher(string);
        Stack<Result> results = new Stack<Result>();
        while (matcher.find()) {
            final MatchResult matchResult = matcher.toMatchResult();
            Result r = new Result();
            r.replace = callback.matches(matchResult);
            if (r.replace == null) {
                continue;
            }
            r.start = matchResult.start();
            r.end = matchResult.end();
            results.push(r);
        }
        // Improve this with a stringbuilder...
        while (!results.empty()) {
            Result r = results.pop();
            string = string.substring(0, r.start) + r.replace + string.substring(r.end);
        }
        return string;
    }

    /**
     * If you wish to reuse the regex multiple times with different callbacks or search strings, you can create a
     * ReplaceCallback directly and use this method to perform the search and replace.
     *
     * @param string    The string we are searching through
     * @param callback  A callback instance that will be applied to the regex match results.
     * @return  The modified search string.
     */
    public String execute(String string, final Callback callback) {
        this.callback = callback;
        return execute(string);
    }

    /**
     * Use this static method to perform your regex search.
     * @param search    The string we are searching through
     * @param regex             The regex to apply to the string
     * @param callback  A callback instance that will be applied to the regex match results.
     * @return  The modified search string.
     */
    public static String find(String search, String regex, int flags, Callback callback) {
        ReplaceCallback rc = new ReplaceCallback(regex, flags, callback);
        return rc.execute(search);
    }
}
