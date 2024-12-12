package ma.emsi.tp1saadmosaif.jsf;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.model.SelectItem;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import ma.emsi.tp1saadmosaif.llm.JsonUtilPourGemini;
import ma.emsi.tp1saadmosaif.llm.LlmInteraction;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Backing bean pour la page JSF index.xhtml.
 * Portée view pour conserver l'état de la conversation pendant plusieurs requêtes HTTP.
 */
@Named
@ViewScoped
public class Bb implements Serializable {

    /**
     * Rôle "système" que l'on attribuera plus tard à un LLM.
     * Valeur par défaut que l'utilisateur peut modifier.
     * Possible d'ajouter de nouveaux rôles dans la méthode getSystemRoles.
     */
    private String systemRole = "helpful assistant";
    /**
     * Quand le rôle est choisi par l'utilisateur dans la liste déroulante,
     * il n'est plus possible de le modifier (voir code de la page JSF).
     */
    private boolean systemRoleChangeable = true;

    /**
     * Dernière question posée par l'utilisateur.
     */
    private String question;
    /**
     * Dernière réponse de l'API OpenAI.
     */
    private String reponse;
    /**
     * La conversation depuis le début.
     */
    private StringBuilder conversation = new StringBuilder();

    /**
     * Contexte JSF. Utilisé pour qu'un message d'erreur s'affiche dans le formulaire.
     */
    @Inject
    private FacesContext facesContext;

    private String texteRequeteJson;

    private String texteReponseJson;

    private boolean debug;

    @Inject
    private JsonUtilPourGemini jsonUtilPourGemini;

    /**
     * Obligatoire pour un bean CDI (classe gérée par CDI).
     */
    public Bb() {
    }

    public String getSystemRole() {
        return systemRole;
    }

    public void setSystemRole(String systemRole) {
        this.systemRole = systemRole;
    }

    public boolean isSystemRoleChangeable() {
        return systemRoleChangeable;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getReponse() {
        return reponse;
    }

    public String getTexteReponseJson() {
        return texteReponseJson;
    }

    public void setTexteReponseJson(String texteReponseJson) {
        this.texteReponseJson = texteReponseJson;
    }

    public String getTexteRequeteJson() {
        return texteRequeteJson;
    }

    public void setTexteRequeteJson(String texteRequeteJson) {
        this.texteRequeteJson = texteRequeteJson;
    }

    public boolean isDebug() {
        return debug;
    }
    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    /**
     * setter indispensable pour le textarea.
     *
     * @param reponse la réponse à la question.
     */
    public void setReponse(String reponse) {
        this.reponse = reponse;
    }

    public String getConversation() {
        return conversation.toString();
    }

    public void setConversation(String conversation) {
        this.conversation = new StringBuilder(conversation);
    }

    public void toggleDebug() {
        this.setDebug(!isDebug());
    }
    /**
     * Envoie la question au serveur.
     * En attendant de l'envoyer à un LLM, le serveur fait un traitement quelconque, juste pour tester :
     * Le traitement consiste à copier la question en minuscules et à l'entourer avec "||". Le rôle système
     * est ajouté au début de la première réponse.
     *
     * @return null pour rester sur la même page.
     */
    public String envoyer() {
        if (question == null || question.isBlank()) {
            // Erreur ! Le formulaire va être automatiquement réaffiché par JSF en réponse à la requête POST,
            // avec le message d'erreur donné ci-dessous.
            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Texte question vide", "Il manque le texte de la question");
            facesContext.addMessage(null, message);
            return null;
        }
        // Entourer la réponse avec "||".
        //this.reponse = "||";
        // Si la conversation n'a pas encore commencé, ajouter le rôle système au début de la réponse
        //if (this.conversation.isEmpty()) {
            // Ajouter le rôle système au début de la réponse
        //    this.reponse += systemRole.toUpperCase(Locale.FRENCH) + "\n";
            // Invalide le bouton pour changer le rôle système
        //    this.systemRoleChangeable = false;
        //}
        //this.reponse += question.toLowerCase(Locale.FRENCH) + "||";

        //Affiche le rôle de l'API
        try {
            LlmInteraction interaction = jsonUtilPourGemini.envoyerRequete(question);
            this.reponse = interaction.reponseExtraite();
            this.texteRequeteJson = interaction.texteRequeteJson();
            this.texteReponseJson = interaction.texteReponseJson();
        } catch (Exception e) {
            FacesMessage message =
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Problème de connexion avec l'API du LLM",
                            "Problème de connexion avec l'API du LLM : " + e.getMessage());
            facesContext.addMessage(null, message);
        }
        afficherConversation();
        return null;
    }

    /**
     * Pour un nouveau chat.
     * Termine la portée view en retournant "index" (la page index.xhtml sera affichée après le traitement
     * effectué pour construire la réponse) et pas null. null aurait indiqué de rester dans la même page (index.xhtml)
     * sans changer de vue.
     * Le fait de changer de vue va faire supprimer l'instance en cours du backing bean par CDI et donc on reprend
     * tout comme au début puisqu'une nouvelle instance du backing va être utilisée par la page index.xhtml.
     * @return "index"
     */
    public String nouveauChat() {
        return "index";
    }

    /**
     * Pour afficher la conversation dans le textArea de la page JSF.
     */
    private void afficherConversation() {
        this.conversation.append("* User:\n").append(question).append("\n* Serveur:\n").append(reponse).append("\n");
    }

    public List<SelectItem> getSystemRoles() {
        List<SelectItem> listeSystemRoles = new ArrayList<>();
        // Ces rôles ne seront utilisés que lorsque la réponse sera données par un LLM.
        String role = """ 
            You are a problem-solving strategist. Users can describe any challenge 
            they're facing - personal, professional, or creative. You'll help them 
            analyze the situation, brainstorm multiple solution approaches, 
            evaluate potential outcomes, and develop a strategic action plan.
            """;
        listeSystemRoles.add(new SelectItem(role, "Strategic Problem Solver"));

        role = """ 
            You are a recipe assistant. Help users find recipes, 
            provide cooking tips, suggest ingredient substitutions, 
            and offer basic cooking techniques for various dishes.
            """;
        listeSystemRoles.add(new SelectItem(role, "Cooking Helper"));

        return listeSystemRoles;
    }
}
