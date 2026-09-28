package searchoteca.mfa;

public final class EmailMasker {
    private EmailMasker() {}

    public static String mask(String email){
        if(email == null || !email.contains("@")) return "***";
        int at = email.indexOf("@");

        String local = email.substring(0,at);
        String domain = email.substring(at);

        if(local.length() <= 2){
            return local.charAt(0) + "***" + domain;
        }
        return local.charAt(0) + "*".repeat(local.length() - 2) + local.charAt(local.length() - 1) + domain;
    }
}
