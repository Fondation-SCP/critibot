package tk.cyriellecentori.CritiBot;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Random;
import java.util.Stack;
import java.util.Vector;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.security.auth.login.LoginException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.Command.Choice;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Modal;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import tk.cyriellecentori.CritiBot.BotCommand.Alias;
import tk.cyriellecentori.CritiBot.Ecrit.InteretType;
import tk.cyriellecentori.CritiBot.Ecrit.Status;
import tk.cyriellecentori.CritiBot.Ecrit.Type;

/**
 * 
 * @author cyrielle
 * Classe principale du bot.
 */
public class CritiBot implements EventListener {
	
	public static void main(String[] args) {
		if(args.length < 1) { // Le token du bot se place en premier param??tre au lancement.
			System.err.println("Merci d'indiquer le token du bot en param??tre.");
			return;
		}
		@SuppressWarnings("unused")
		CritiBot cb = new CritiBot(args[0]);

	}
	
	public static Random random = new Random();
	
	/**
	 * Date de la derni??re v??rification des flux RSS.
	 */
	private long lastCheck = 1620570510854L;
	/**
	 * Liste des entr??es que le bot n'a pas pu interpr??ter.
	 */
	private Vector<SyndEntry> inbox = new Vector<SyndEntry>();
	/**
	 * Le token du bot pour se connecter ?? l'utilisateur Critibot.
	 */
	private String token;
	
	private JDABuilder builder;
	public JDA jda;
	
	private Gson gson;
	
	/**
	 * L'historique des ??tats, permet de faire des retours en arri??re.
	 */
	private Stack<Vector<Ecrit>> cancel = new Stack<Vector<Ecrit>>();
	/**
	 * La base de donn??es des ??crits.
	 */
	private Vector<Ecrit> ecrits;
	/**
	 * S'il y a eu une erreur lors d'une tentative de sauvegarde automatique de la base de donn??es.
	 */
	private boolean errorSave = false;
	/**
	 * Liste des commandes.
	 */
	private LinkedHashMap<String, BotCommand> commands = new LinkedHashMap<String, BotCommand>();
	
	private Affichan[] affichans;
	
	/**
	 * L'emote de r??servation.
	 */
	public final long henritueur;
	/**
	 * L'emote indiquant l'??crit comme critiqu??.
	 */
	public final String henricheck = "U+1f4e8";
	/**
	 * L'emote indiquant l'??crit comme refus??.
	 */
	public final String henricross = "U+274c";
	//private final String whiteCheckBox = "U+2705";
	/**
	 * L'emoji cadenas ouvert.
	 */
	public final String unlock = "U+1f513";
	//private final String cross = "U+274e";
	/**
	 * Le pr??fixe du bot.
	 */
	private final String prefix;
	/**
	 * La date de la derni??re mise ?? jour manuelle du bot.
	 */
	public long lastUpdate;
	
	/**
	 * Le salon ????Organisation????
	 */
	public final long organichan;
	
	/*
	 * Si le bot est boot sur le compte b??ta ou non
	 */
	public final boolean beta;
	
	public CritiBot(String token) {
		this.token = token;
		if(token.hashCode() == 1973164890) { // Si le bot est connect?? ?? l'utilisateur Critibot#8684, les salons sont ceux du discord des Critiqueurs.
			beta = false;
			prefix = "c";
			henritueur = 844249814799351838L;
			organichan = 614947463610236939L;
			affichans = new Affichan[] {
					new Affichan(843956373103968308L, new Status[] {Status.OUVERT, Status.OUVERT_PLUS}, null, null),
					new Affichan(614947463610236939L, new Status[] {Status.OUVERT_PLUS}, null, null),
					new Affichan(896361827884220467L, new Status[] {Status.INCONNU, Status.INFRACTION}, null, null),
					new Affichan(896362452818747412L, null, new Type[] {Type.AUTRE}, null),
					new Affichan(554998005850177556L, new Status[] {Status.OUVERT, Status.OUVERT_PLUS}, null, new String[] {"Concours", "Validation"})
			};
		} else { // Sinon, le bot est consid??r?? comme ??tant en b??ta et les salons sont ceux de BrainBot's lair.
			beta = true;
			prefix = "bc";
			henritueur = 470138432723877888L;
			organichan = 737725144390172714L;
			affichans = new Affichan[] {
					new Affichan(878917114474410004L, new Status[] {Status.OUVERT, Status.OUVERT_PLUS}, null, null),
					new Affichan(737725144390172714L, new Status[] {Status.OUVERT_PLUS}, null, null),
					//new Affichan(896361827884220467L, new Status[] {Status.INCONNU, Status.INFRACTION}, null, null),
					//new Affichan(896362452818747412L, null, new Type[] {Type.AUTRE}, null),
					//new Affichan(901072726456930365L, new Status[] {Status.OUVERT, Status.OUVERT_PLUS}, null, new String[] {"Concours", "Validation"})
			};
			System.out.println("Booting in beta.");
		}
		
		// Initialisation du moteur JSON
		GsonBuilder gsonBilder = new GsonBuilder();
		gsonBilder.setPrettyPrinting(); // Histoire d'avoir un beau JSON

		gson =gsonBilder.create();

		// Lecture de la base de donn??es
		String data = "";
		BufferedReader dataFile;
		try {
			dataFile = new BufferedReader(new FileReader("/home/cyrielle/bots/critibot.json"));
			while(true) {
				String str = dataFile.readLine();
				if(str == null) break;
				else data = data + "\n" + str;
			}

			dataFile.close();

		} catch (IOException exp) {
			exp.printStackTrace();
			System.out.println("Impossible d'ouvrir les donn??es, de nouvelles seront cr??es ?? la prochaine sauvegarde.");
			ecrits = new Vector<Ecrit>();
		}

		TypeToken<Vector<Ecrit>> ttve = new TypeToken<Vector<Ecrit>>() {};

		// ?? la fin du fichier se trouve normalement la date de la derni??re recherche de mise ?? jour des flux RSS
		try {
			lastCheck = Long.parseLong(data.split("??")[1]);
			ecrits = gson.fromJson(data.split("??")[0], ttve.getType());
		} catch(Exception e) { // Si ce n'est pas le cas, tant pis
			ecrits = gson.fromJson(data, ttve.getType());
		}



		// S'il y a une erreur dans l'initialisation des donn??es, ??viter les NullPointerException
		if(ecrits == null)
			ecrits = new Vector<Ecrit>();

		// V??rification que tous les ??crits sont bien, toujours pour ??viter les NullPointerException
		for(Ecrit e : ecrits) {
			e.check();
		}

		// Bout de code qui supprime un nombre al??atoire d'??crits de la BDD (50% en moyenne).
		/*Vector<Ecrit> nouveau = new Vector<Ecrit>();
				java.util.Random r = new java.util.Random();
				for(Ecrit e : ecrits) {
					if(r.nextBoolean()) {
						nouveau.add(e);
					}
				}
				ecrits = nouveau;
				try {
					save();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				System.exit(0);*/

		lastUpdate = System.currentTimeMillis();

		// Initialisation de l'API
		try {

			builder = JDABuilder.createDefault(this.token)
					.addEventListeners(this);

			jda = builder.build();
		} catch(LoginException | IllegalArgumentException e) {
			e.printStackTrace();
			return;
		}
		jda.setAutoReconnect(true);


	}

	/**
	 * Met ?? jour la BDD avec les flux RSS.
	 * @throws IllegalArgumentException
	 * @throws MalformedURLException
	 * @throws FeedException
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	public void addNew() throws IllegalArgumentException, MalformedURLException, FeedException, IOException {
		// R??cup??re le flux
		SyndFeed feed = new SyndFeedInput().build(new XmlReader(new URL("http://fondationscp.wikidot.com/feed/forum/ct-656675.xml")));
		// Date du flux le plus r??cent
		Date lastDate = new Date(0);
		// V??rification de toutes les entr??es
		for(Object e : feed.getEntries()) {
			SyndEntry entry = (SyndEntry) e;
			
			if(entry.getPublishedDate().after(new Date(lastCheck))) { // V??rifie si le flux est nouveau
				if(entry.getTitle().contains("]")) { // V??rifie si le titre a des balises
					Type type = null;
					// R??cup??re la liste des balises
					String[] sp = entry.getTitle().split("]");
					String balises = "";
					for(int i = 0; i < sp.length - 1; i++) {
						balises += sp[i] + "]";
					}
					// Nettoyage du titre (retire les SCP-XXX-FR devant, les espaces inutiles et autres trucs pas jolis).
					String unclean = sp[sp.length - 1];
					if(unclean.contains("SCP") && unclean.contains("FR")) {
						unclean = unclean.split("FR",2)[1];
					}
					while(unclean.startsWith(" ") || unclean.startsWith(":")) {
						unclean = unclean.substring(1);
					}
					if(unclean.startsWith("\"")) {
						unclean = unclean.substring(1, unclean.length() - 1);
					}
					// Si pas de titre propre, l'id??e devient un ????sans nom????.
					if(unclean.isEmpty() || sp.length == 1) {
						Vector<Ecrit> es = searchEcrit("sans nom");
						unclean = "(sans nom " + es.size() + ")";
						balises = entry.getTitle();
					}
					// Tentative de d??duction du type.
					if(balises.contains("Id??e") || balises.contains("id??e")) {
						type = Type.IDEE;
					} else if(balises.contains("Conte") || balises.contains("S??rie") || balises.contains("conte")) {
						type = Type.CONTE;
					} else if(balises.contains("Refus")) { // Si l'id??e est cr????e avec une balise ????Refus????, wtf
						type = Type.AUTRE;
					} else { // Par d??faut, juste ????Critiques???? c'est un rapport.
						type = Type.RAPPORT;
					}
					
					// Cr??e l'??crit et l'ajoute ?? la BDD
					Ecrit ecrit = new Ecrit(unclean, entry.getLink(), type, Status.OUVERT, ((ArrayList<org.jdom.Element>) entry.getForeignMarkup()).get(0).getText());
					ecrits.add(ecrit);
					
				} else { // Si pas de balises, d??j?? PUTAIN??LES??BALISES??BORDEL, ensuite ajoute ?? la inbox.
					inbox.add(entry);
					jda.getPresence().setStatus(OnlineStatus.IDLE); // Le signale par un statut orange.
				}
			}
			if(lastDate.before(entry.getPublishedDate())) // Garde la date la plus r??cente des fils regard??s.
				lastDate = entry.getPublishedDate();
		}
		// Met ?? jour la date de derni??re v??rification des flux
		if(lastDate.getTime() != 0) {
			lastCheck = lastDate.getTime() + 1;
		}
	}
	
	/**
	 * Sauvegarde la BDD.
	 * @throws IOException
	 */
	public void save() throws IOException {
		BufferedWriter dataFile = new BufferedWriter(new FileWriter("/home/cyrielle/bots/critibot.json"));
		dataFile.write(gson.toJson(ecrits) + "??" + lastCheck);
		dataFile.close();
	}
	
	/**
	 * Rend les cha??nes de caract??res plus ????basiques???? en supprimant les majuscules et les diacritiques.
	 * @param s La cha??ne ?? simplifier.
	 * @return Une cha??ne de caract??res simple, seulement en minuscules sans diacritiques.
	 */
	static public String basicize(String s) {
		return s.toLowerCase()
				.replaceAll("??", "e")
				.replaceAll("??", "e")
				.replaceAll("??", "a")
				.replaceAll("??", "i")
				.replaceAll("??", "c")
				.replaceAll("??", "e")
				.replaceAll("??", "o")
				.replaceAll("??", "e")
				.replaceAll("??", "i")
				.replaceAll("??", "oe")
				.replaceAll("??", "ae")
				.replaceAll("??", "u")
				.strip()
				.replaceAll("??", "a")
				.replaceAll("??", "o")
				.replaceAll("??", "a")
				.replaceAll("???", "'");
		
	}
	
	/**
	 * Recherche les cha??nes de carat??res correspondant au crit??re dans le vecteur donn??.
	 * @param s crit??re
	 * @param content vecteur ?? chercher
	 * @return Un vecteur contenant les identifiant dans le vecteur content des cha??nes de carat??re correspondant au crit??re.
	 */
	static public Vector<Integer> search(String s, Vector<String> content) {
		Vector<Integer> ret = new Vector<Integer>();
		String[] motsSearch = basicize(s).split(" ");
		for(int i = 0; i < content.size(); i++) { // V??rifie si chaque cha??ne de caract??res respecte le crit??re.
			String[] mots = basicize(content.get(i)).split(" "); // Simplifie la cha??ne et s??pare ses mots.
			boolean ok = true;
			for(String motSearch : motsSearch) { // V??rifie si un mot contient un mot du crit??re
				boolean found = false;
				for(String mot : mots) { 
					if(mot.toLowerCase().contains(motSearch.toLowerCase())) {
						found = true;
					}
				}
				if(!found) { // Si un mot du crit??re n'est pas pr??sent dans un mot de la cha??ne, alors elle ne correspond pas au crit??re.
					ok = false;
					break;
				}
			}
			if(ok) {
				ret.add(i);
			}
		}
		return ret;
	}
	
	/**
	 * Recherche les ??crits avec de nombreux crit??res.
	 * @param critere Le crit??re sur le titre ("" pour indiff??rent)
	 * @param status Liste des status recherch??s (vide pour indiff??rent)
	 * @param type Liste des types recherch??s (vide pour indiff??rent)
	 * @param authors Liste des auteurs recherch??s (vide pour indiff??rent)
	 * @param tags Liste des tags recherch??s (vide pour indiff??rent)
	 * @param tagAnd Si l'??crit doit avoir l'un des tags ou tous les tags
	 * @param modAvant Date de plus r??cente modification (0 pour indiff??rent)
	 * @param modApres Date de plus ancienne modification (0 pour indiff??rent)
	 * @return Un vecteur des ??crits correspondant aux crit??res.
	 */
	public Vector<Ecrit> ulister(String critere, Vector<Status> status, Vector<Type> type, Vector<String> authors, Vector<String> tags, boolean tagAnd, long modAvant, long modApres) {
		Vector<Ecrit> candidats = new Vector<Ecrit>();
		if(critere.isEmpty()) {
			candidats = ecrits;
		} else {
			candidats = searchEcrit(critere);
		}
		Vector<Ecrit> choisis = new Vector<Ecrit>();
		for(Ecrit e : candidats) {
			boolean ok = true;
			if(!status.isEmpty())
				ok = ok && status.contains(e.getStatus());
			if(!type.isEmpty())
				ok = ok && type.contains(e.getType());
			if(!authors.isEmpty())
				ok = ok && authors.contains(e.getAuteur());
			if(!tags.isEmpty()) {
				boolean okTag = tagAnd;
				for(String tag : tags) {
					if(tagAnd) {
						okTag = okTag && e.hasTag(tag);
					} else {
						okTag = okTag || e.hasTag(tag);
					}
				}
				ok = ok && okTag;
			}
			if(modAvant != 0) {
				ok = ok && (e.getLastUpdateLong() < modAvant);
			}
			if(modApres != 0) {
				ok = ok && (e.getLastUpdateLong() > modApres);
			}
			if(ok) {
				choisis.add(e);
			}
		}
		return choisis;
	}
	
	/**
	 * Lance une recherche parmi les ??crits. Le crit??re doit ??tre consitu??e de parties de mots du titre, majuscules et diacritiques ignor??es.
	 * @param s Le crit??re de recherche.
	 * @return Une liste d'??crits correspondant au crit??re.
	 */
	public Vector<Ecrit> searchEcrit(String s) {
		Vector<Ecrit> list = new Vector<Ecrit>();
		// Cr??e un tableau des noms des ??crits
		Vector<String> names = new Vector<String>();
		for(Ecrit e : ecrits) {
			names.add(e.getNom());
		}
		// R??cup??re les indices des ??crits correspondant aux crit??re et les ajoute au tableau de retour
		for(int index : search(s, names)) {
			list.add(ecrits.get(index));
		}
		return list;
	}
	
	/**
	 * Lance une recherche d'auteurs. Le crit??re doit ??tre consitu??e de parties de mots du nom de l'auteur, majuscules et diacritiques ignor??es.
	 * @param s Le crit??re de recherche.
	 * @return Une liste d'auteurs correspondant au crit??re.
	 */
	public Vector<String> autSearch(String s) {
		Vector<String> list = new Vector<String>();
		// R??cup??re la liste de tous les auteurs de la base de donn??es.
		Vector<String> auteurList = new Vector<String>();
		for(Ecrit e : ecrits) {
			if(!auteurList.contains(e.getAuteur())) {
				auteurList.add(e.getAuteur());
			}
		}
		// Ajoute les auteurs correspondant au crit??re dans la liste des r??sultats
		for(int index : search(s, auteurList)) {
			list.add(auteurList.get(index));
		}
		return list;
	}
	
	/**
	 * Nettoie la base de donn??es en supprimant tous les ??crits consid??r??s comme ????morts???? (publi??s, refus??s ou abandonn??s).
	 * @param fort Si `true`, supprime ??galement les ??crits sans nouvelles.
	 */
	public void clean(boolean fort) {
		// Liste des ??crits ?? supprimer.
		Vector<Ecrit> toRem = new Vector<Ecrit>();
		for(Ecrit e : ecrits) { // V??rifie chaque ??crit.
			if(e.isDead() || (fort && e.getStatus() == Status.SANS_NOUVELLES))
				toRem.add(e);
		}
		// Supprime les ??crits ?? supprimer.
		for(Ecrit e : toRem) {
			ecrits.remove(e);
		}
	}
	
	/*
	 * Sends embeds in a MessageChannel
	 */
	public void sendEmbeds(MessageChannel chan, Vector<MessageEmbed> embeds) {
		String id = new String("mm").concat(Long.toString(System.currentTimeMillis()));
		if(embeds.size() > 1) {
			this.multimessages.put(id, embeds);
			this.mmposition.put(id, 0);
			chan.sendMessageEmbeds(embeds.firstElement()).setActionRow(
					Button.secondary(id.concat("-p"), "Pr??c??dent").asDisabled(), 
					Button.secondary(id.concat("-n"), "Suivant")).queue();
		} else {
			chan.sendMessageEmbeds(embeds.firstElement()).queue();
		}
	}
	
	/*
	 * Sends embeds in response of a slash command
	 */
	public void sendEmbeds(SlashCommandInteractionEvent event, Vector<MessageEmbed> embeds) {
		String id = new String("mm").concat(Long.toString(System.currentTimeMillis()));
		if(embeds.size() > 1) {
			this.multimessages.put(id, embeds);
			this.mmposition.put(id, 0);
			event.replyEmbeds(embeds.firstElement()).addActionRow(
					Button.secondary(id.concat("-p"), "Pr??c??dent").asDisabled(), 
					Button.secondary(id.concat("-n"), "Suivant")).queue();
		} else {
			event.replyEmbeds(embeds.firstElement()).queue();
		}
	}
	
	/**
	 * Supprime l'??crit donn?? de la base de donn??es.
	 * @param e ??crit ?? supprimer.
	 */
	public void remove(Ecrit e) {
		ecrits.remove(e);
	}
	
	/**
	 * V??rifie que tous les ??crits ouverts et r??serv??s soient pr??sents dans leur salon d??di??.
	 */
	public void updateOpen() {
		for(Affichan aff : affichans) {
			aff.update(this);
		}
		for(Ecrit e : ecrits) {
			e.edited = false;
		}
	}
	
	/**
	 * Supprime tous les messages des ??crits. updateOpen() ??tant appel?? ?? chaque commande, tous les messages seront recr??es juste apr??s.
	 */
	public void refreshMessages() {
		for(Affichan aff : affichans) {
			aff.purge(jda);
			aff.update(this);
		}
	}
	
	/**
	 * M??thode de fusion pour le tri fusion qui suit.
	 */
	public Vector<Ecrit> merge(Vector<Ecrit> a, Vector<Ecrit> b) {
		Vector<Ecrit> res = new Vector<Ecrit>();
		int i = 0,j = 0;
		while(i < a.size() && j < b.size()) {
			if(a.get(i).getLastUpdateLong() < b.get(j).getLastUpdateLong()) {
				res.add(a.get(i));
				i++;
			} else {
				res.add(b.get(j));
				j++;
			}
		}
		for(; i < a.size(); i++) {
			res.add(a.get(i));
		}
		for(; j < b.size(); j++) {
			res.add(b.get(j));
		}
		return res;
	}
	
	/**
	 * Trie les ??crits par ordre croissant de date de derni??re modification.
	 * Tri utilis????: tri fusion.
	 */
	public Vector<Ecrit> sortByDate(Vector<Ecrit> toSort) {
		if(toSort.size() < 2)
			return toSort;
		return merge(sortByDate(new Vector<Ecrit>(toSort.subList(0, toSort.size() / 2))), 
				sortByDate(new Vector<Ecrit>(toSort.subList(toSort.size() / 2, toSort.size()))));
	}
	
	/**
	 * Initialise et liste les commandes.
	 */
	private void initCommands() {
		Vector<Command.Choice> choicesStatus = new Vector<Command.Choice>();
		for(String s : Status.names) {
			choicesStatus.add(new Command.Choice(s, s));
		}
		
		Vector<Command.Choice> choicesTypes = new Vector<Command.Choice>();
		for(String s : Type.names) {
			choicesTypes.add(new Command.Choice(s, s));
		}
		
		Vector<Command.Choice> choicesMarques = new Vector<Command.Choice>();
		choicesMarques.add(new Command.Choice("Critique imm??diate", "instant"));
		choicesMarques.add(new Command.Choice("R??servation exclusive", "seul"));
		choicesMarques.add(new Command.Choice("R??servation ouverte", "ouvert"));
		choicesMarques.add(new Command.Choice("Simple int??r??t", "longterme"));
		choicesMarques.add(new Command.Choice("Collaboration recherch??e", "collab"));
		
		OptionData typeOption = new OptionData(OptionType.STRING, "type", "Le type de l?????crit").addChoices(choicesTypes);
		OptionData statutOption = new OptionData(OptionType.STRING, "statut", "Le statut de l?????crit").addChoices(choicesStatus);
		OptionData ecritOption = new OptionData(OptionType.STRING, "ecrit", "L?????crit cibl??");
		OptionData idOption = new OptionData(OptionType.BOOLEAN, "recherche-id", "Si la recherche se fait par identifiant");
		OptionData dateOption = new OptionData(OptionType.STRING, "date", "Date au format jj/mm/aaaa");
		
		for(Command c : jda.retrieveCommands().complete()) {
			System.out.println("Suppression de la commande " + c.getName() + "???");
			c.delete().complete();
		}
		
		commands.put("aide", new BotCommand(this, "aide", "Affiche la liste des commandes et leur mode d???emploi.") {
			MessageEmbed message = null;
			
			public void load() {
				EmbedBuilder b = new EmbedBuilder();
				b.setTitle("Aide de Critibot");
				b.setDescription("Les param??tres entre crochets sont optionnels, entre accolades obligatoires. Il est ??galement possible d???utiliser les commandes en texte avec le pr??fixe ?????c!????? et __en s??parant les options par point-virgule au lieu d???un espace__. La description des options est disponible en description des commandes slash.");
				b.addField("Valeurs de Statut", "Statut doit ??tre??????Ouvert ??? Ouvert* ??? En attente ??? Abandonn?? ??? En pause ??? Sans nouvelles ??? Inconnu ??? Publi?? ??? R??serv?? ??? Valid??????? Refus??????? Infraction????. ????Ouvert*???? correspond aux ??crits ouverts poss??dant des marques d'int??r??t, il ne peut ??tre assign?? manuellement.", false);
				b.addField("Valeurs de Type", "Type doit ??tre ????Conte ??? Rapport ??? Id??e ??? Format GdI ??? Autre????", false);
				b.addField("Commandes de base", "`/aide`??: Cette commande d'aide.\n"
						+ "`/annuler`??: Annule la derni??re modification effectu??e.", false);
				b.addField("Commandes de gestion et d'affichage de la liste", "`/ajouter {Nom} {Auteur} {Type} {Statut} {URL}`??: Ajoute manuellement un ??crit ?? la liste.\n"
						+ "`/supprimer {Crit??re}` : Supprime un ??crit. Le Crit??re doit ??tre assez fin pour aboutir ?? un unique ??crit. __**ATTENTION**__??: Il n'y a pas de confirmation, faites attention ?? ne pas vous tromper dans le Crit??re.\n"
						+ "`/inbox`??: Affiche la bo??te de r??ception, contenant les ??crits qui n'ont pas pu ??tre ajout??s automatiquement. Attention, l'appel ?? cette commande supprime le contenu de la bo??te.", false);
				b.addField("Commandes de recherche", "`/rechercher??{Crit??re}`??: Affiche tous les ??crits contenant {Crit??re}.\n"
						+ "`/lister {Statut} [Type]`??: Affiche la liste des ??crits avec le statut et du type demand??s. Statut et Type peuvent prendre la valeur ????Tout????.\n"
						+ "`/lister_tags`??: Affiche tous les tags existants dans la base de donn??es et le nombre d'??crits y ??tant associ??s", false);
				b.addField("Commandes de critiques", "`/marquer {Crit??re}`??: Ajoute une marque d'int??r??t ?? un ??crit. Le Crit??re doit ??tre assez fin pour aboutir ?? un unique ??crit.\n"
						+ "`/marquer_pour {Crit??re} {Nom} [Type de marque]`??: Marque d'int??r??t un ??crit pour quelqu'un d'autre. Le Crit??re doit ??tre assez fin pour aboutir ?? un unique ??crit."
						+ "`/lib??rer {Crit??re}`??: Supprime votre marque d'int??t??t sur un ??crit. Le Crit??re doit ??tre assez fin pour aboutir ?? un unique ??crit.\n"
						+ "`/lib??rer_pour {Crit??re} {Nom}`??: Supprime l'int??ret de qulequ'un sur un ??crit. Le Crit??re doit ??tre assez fin pour aboutir ?? un unique ??crit."
						+ "`/up {Crit??re}`??: Marque un ??crit ouvert et le remet au premier plan dans le salon des fils ouverts s'il l'??tait d??j??. Le Crit??re doit ??tre assez fin pour aboutir ?? un unique ??crit.\n"
						+ "`/valider {Crit??re}`??: Change le type du rapport en Rapport si c'??tait une id??e et fait le m??me effet que /critiqu??. Le Crit??re doit ??tre assez fin pour aboutir ?? un unique ??crit.", false);
				b.addField("Commandes de modification d'un ??crit",  "`/statut {Crit??re} {Statut}` : Change le statut de l'??crit demand??. Le Crit??re doit ??tre assez fin pour aboutir ?? un unique ??crit.\n"
						+ "`/type {Crit??re} {Type}` : Change le type de l'??crit demand??. Le Crit??re doit ??tre assez fin pour aboutir ?? un unique ??crit.\n"
						+ "`/auteur {Crit??re} {Auteur}`??: Change l'auteur de l'??crit demand??. Le Crit??re doit ??tre assez fin pour aboutir ?? un unique ??crit."
						+ "`/ouvrir {Crit??re}`??: Raccourci pour `/statut {Crit??re} Ouvert`.\n"
						+ "`/renommer {Crit??re} {Nouveau nom}`??: Renomme un ??crit.\n"
						+ "`/ajouter_tag {Crit??re} {Tag}`??: Ajoute un tag ?? l'??crit.\n"
						+ "`/retirer_tag {Crit??re} {Crit??re tag}`??: Retire un tag ?? l'??crit.", false);
				b.addField("Commandes d'entretien de la base de donn??es??(?? utiliser avec pr??caution)",
						"`/nettoyer`??: Supprime tous les ??crits abandonn??s / refus??s / publi??s de la liste.\n"
						+ "`/archiver_avant {Date}`??: Met le statut ????sans nouvelles???? ?? tous les ??crits n'ayant pas ??t?? mis ?? jour avant la date indiqu??e. La date doit ??tre au format dd/mm/yyyy.\n"
						+ "`/nettoyer_fort`??: Supprime tous les ??crits abandonn??s / refus??s / publi??s / sans nouvelles de la liste.\n"
						+ "`/doublons`??: Supprime les ??ventuels doublons.", false);
				b.addField("Commandes de choix d'??crit",
						"`/al??atoire [Type1];[Type2];???`??: Choisit un ??crit ouvert al??atoire de l'un des types donn??s en param??tre. Si aucun argument n'est donn??, chosit un ??crit ouvert al??atoire sans distinction de type.\n"
						+ "`/ancien [Type1];[Type2];???`??: Choisit l'??crit le plus anciennement modifi?? encore ouvert parmi tous les ??crits des types donn??es en param??tre. Si aucun argument n'est donn??, choisit l'??crit encore ouvert le plus ancien sans distinction de type.", false);
				b.addField("Recherche avanc??e", "La recherche avanc??e est utilisable avec `/ulister`. En mode texte, chaque param??tre est de la forme `nom=valeur`. Les diff??rents param??tres disponibles sont??:\n"
						+ "`nom={Crit??re}`??: R??duit la recherche aux ??crits dont le nom correspond au crit??re.\n"
						+ "`statut={Statut},{Statut},???`??: Les ??crits doivent avoir l'un des statuts de la liste.\n"
						+ "`type={Type},{Type},???`??: Les ??crits doivent avoir l'un des types de la liste.\n"
						+ "`auteur={Crit??re auteur},{Crit??re auteur},???`??: Les ??crits doivent ??tre d'un des auteurs de la liste.\n"
						+ "`tag={Crit??re tag},{Crit??re tag},???`??: Les ??crits doivent poss??der l'un des tags de la liste.\n"
						+ "`tag&={Crit??re tag},{Crit??re tag},???`??: Les ??crits doivent poss??der tous les tags de la liste.\n"
						+ "`avant=jj/mm/aaaa`??: Les ??crits doivent avoir ??t?? modifi??s pour la derni??re fois avant la date indiqu??e.\n"
						+ "`apr??s=jj/mm/aaaa`??: Les ??cirts doivent avoir ??t?? modifi??s pour la derni??re fois apr??s la date indiqu??e.\n", false);
				b.addField("Code source", "Disponible sur [Github](https://github.com/Fondation-SCP/critibot).", false);
				b.setFooter("Version 3.1");
				b.setAuthor("Critibot", null, "https://media.discordapp.net/attachments/719194758093733988/842082066589679676/Critiqueurs5.jpg");
				message = b.build();
			}
			
			public void execute(CritiBot bb, MessageReceivedEvent message, String[] args) {
				if(this.message == null)
					load();
				message.getChannel().sendMessageEmbeds(this.message).queue();
			}

			@Override
			public void slash(CritiBot bot, SlashCommandInteractionEvent event) {
				if(this.message == null) load();
				event.replyEmbeds(message).queue();
			}
		});
		
		commands.put("help", new BotCommand.Alias(this, "help", commands.get("aide")));
		
		commands.put("ajouter", new BotCommand(this, "ajouter", "Ajoute un ??crit ?? la base de donn??es.",
				new OptionData(OptionType.STRING, "nom", "Nom de l?????crit").setRequired(true),
				new OptionData(OptionType.STRING, "auteur", "Auteur de l?????crit").setRequired(true),
				typeOption.setRequired(true),
				statutOption.setRequired(true),
				new OptionData(OptionType.STRING, "url", "L???URL de l?????crit").setRequired(true)) {
			
			
			
			@Override
			public void execute(CritiBot bot, MessageReceivedEvent message, String[] args) {
				archiver();
				try {
					ecrits.add(new Ecrit(args[0],  args[4], Type.getType(args[2]), Status.getStatus(args[3]), args[1]));
					message.getChannel().sendMessage("Ajout????!").queue();
				} catch (ArrayIndexOutOfBoundsException e) {
					message.getChannel().sendMessage("Utilisation??: c!add Nom;Auteur;Type;Statut;URL.").queue();
				}
			}

			@Override
			public void slash(CritiBot bot, SlashCommandInteractionEvent event) {
				archiver();
				ecrits.add(new Ecrit(event.getOption("nom").getAsString(), event.getOption("url").getAsString(), 
						Type.getType(event.getOption("type").getAsString()), Status.getStatus(event.getOption("statut").getAsString()),
						event.getOption("auteur").getAsString()));
				event.reply("Ajout?????!").queue();
			}
			
		});
		
		commands.put("lister", new BotCommand(this, "lister", "Liste tous les ??crits d???un certain type ou statut.",
				typeOption, statutOption) {

			public Vector<MessageEmbed> search(Status status, Type type) {
				// Vector des messages ?? envoyer.
				Vector<MessageEmbed> embeds = new Vector<MessageEmbed>();
				// Vector du contenu des embeds.
				Vector<String> messages = new Vector<String>();
				// S??pare les r??sultats de la recherche pour que chaque message n'exc??de pas les 2??000 caract??res.
				String buffer = "";
				try {
					for(Ecrit e : sortByDate(ecrits)) { // Recherche tous les ??crits respectant les crit??res demand??s et les ajoute aux r??sultats.
						if(e.complyWith(type, status)) {
							// Remplit d'abord un buffer puis lorsqu'il est trop grand, ajoute son contenu dans ????messages???? et le vide.
							String toAdd = "[**" + e.getNom() + "**](" + e.getLien() + ")\n" + e.getAuteur() + "\n" + e.getStatus() + " ??? " + e.getType() + "\n\n";
							if(buffer.length() + toAdd.length() > 1000) {
								messages.add(buffer);
								buffer = "";
							}
							buffer += toAdd;
						}
							
					}
				} catch(IndexOutOfBoundsException e) {
					e.printStackTrace();
				}
				if(buffer.isEmpty()) { // Si le buffer est vide, c'est qu'il n'a jamais ??t?? rempli??: aucun r??sultat, donc.
					EmbedBuilder b = new EmbedBuilder();
					b.setTitle("Aucun r??sultat");
					b.setAuthor("Recherche??: " + ((status == null) ? "Tous statuts" : status) + " ??? " + ((type == null) ? "Tous types" : type));
					b.setTimestamp(Instant.now());
					b.setColor(16001600);
					embeds.add(b.build());
				} else { // Sinon, envoie les r??sultats.
					messages.add(buffer); // Ajoute le buffer restant aux messages.
					EmbedBuilder b = new EmbedBuilder();
					b.setTitle("R??sultats de la recherche");
					b.setAuthor("Recherche??: " + ((status == null) ? "Tous statuts" : status) + " ??? " + ((type == null) ? "Tous types" : type));
					b.setTimestamp(Instant.now());
					b.setColor(73887);
					for(int i = 0; i < messages.size(); i++) { // Cr??e les diff??rents messages ?? envoyer en num??rotant les pages.
						b.setFooter("Page " + (i + 1) + "/" + messages.size());
						b.setDescription(messages.get(i));
						embeds.add(b.build());
					}
				}
				return embeds;
			}
			
			@Override
			public void execute(CritiBot bot, MessageReceivedEvent message, String[] args) {
				try {
					// Les ??num??rations restent ????null???? si tout est accept??.
					Status status = (args[0].equalsIgnoreCase("tout") ? null : Status.getStatus(args[0]));
					Type type = null;
					if(args.length > 1) // Gestion du param??tre optionnel de type.
						type = (args[1].equalsIgnoreCase("tout") ? null : Type.getType(args[1]));
					sendEmbeds(message.getChannel(), search(status, type));
				} catch(ArrayIndexOutOfBoundsException e) {
					message.getChannel().sendMessage("Utilisation??: c!lister Statut;Type. Il est possible de ne pas sp??cifier le type si non n??cessaire, alors la commande sera c!list Statut et tous les types seront affich??s.").queue();
				}
			}

			@Override
			public void slash(CritiBot bot, SlashCommandInteractionEvent event) {
				OptionMapping statusO = event.getOption("statut");
				OptionMapping typeO = event.getOption("type");
				Status status = (statusO == null) ? null : Status.getStatus(statusO.getAsString());
				Type type = (typeO == null) ? null : Type.getType(typeO.getAsString());
				sendEmbeds(event, search(status, type));
			}
			
		});
		commands.put("l", new BotCommand.Alias(commands.get("lister")));
		
		commands.put("plop", new BotCommand() {

			@Override
			public void execute(CritiBot bot, MessageReceivedEvent message, String[] args) {
				message.getChannel().sendMessage("plop").queue();
				
			}

			@Override
			public void slash(CritiBot bot, SlashCommandInteractionEvent event) {
				// TODO Auto-generated method stub
				
			}
			
		});
		
		commands.put("rechercher", new BotCommand(this, "rechercher", "Recherche des ??crits par nom.", new OptionData(OptionType.STRING, "nom", "Nom").setRequired(true)) {

			public Vector<MessageEmbed> search(Vector<Ecrit> res, String critere) {
				Vector<MessageEmbed> embeds = new Vector<MessageEmbed>();
				if(res.isEmpty()) { // S'il n'y a aucun r??sultat.
					EmbedBuilder b = new EmbedBuilder();
					b.setTitle("Aucun r??sultat");
					b.setAuthor("Recherche??: " + critere);
					b.setTimestamp(Instant.now());
					b.setColor(16001600);
					embeds.add(b.build());
				} else { // Sinon, afficher une liste similaire ?? celle de c!lister. Voir cette commande pour plus de commentaires.
					Vector<String> messages = new Vector<String>();
					String buffer = "";
					for(Ecrit e : res) {
						String toAdd = "[**" + e.getNom() + "**](" + e.getLien() + ")\n" + e.getAuteur() + "\n" + e.getStatus() + " ??? " + e.getType() + "\n\n";
						if(buffer.length() + toAdd.length() > 1000) {
							messages.add(buffer);
							buffer = "";
						}
						buffer += toAdd;
					}
					messages.add(buffer);
					EmbedBuilder b = new EmbedBuilder();
					b.setTitle("R??sultats de la recherche");
					b.setAuthor("Recherche??: " + critere);
					b.setTimestamp(Instant.now());
					b.setColor(73887);
					for(int i = 0; i < messages.size(); i++) {
						b.setFooter("Page " + (i + 1) + "/" + messages.size());
						b.setDescription(messages.get(i));
						embeds.add(b.build());
					}

				}
				return embeds;
			}
			
			@Override
			public void execute(CritiBot bot, MessageReceivedEvent message, String[] args) {
				try {
						Vector<Ecrit> res =  bot.searchEcrit(args[0]);
						if(res.size() <= 3 && !res.isEmpty()) {
							for(Ecrit e : res)
								Affichan.sendMessageWithActions(e, message.getTextChannel()).queue();
						} else {	
							sendEmbeds(message.getChannel(), search(res, args[0]));
						}
						
				} catch(ArrayIndexOutOfBoundsException e) {
					message.getChannel().sendMessage("Rechercher??? Quoi exactement???").queue();
				}
			}

			@Override
			public void slash(CritiBot bot, SlashCommandInteractionEvent event) {
				Vector<Ecrit> res =  bot.searchEcrit(event.getOption("nom").getAsString());
				if(res.size() <= 3 && !res.isEmpty()) {
					event.deferReply().queue();
					for(Ecrit e : res)
						Affichan.sendMessageWithActions(e, event.getHook()).queue();
				} else {	
					sendEmbeds(event, search(res, event.getOption("nom").getAsString()));
				}
			}

		});
		
		commands.put("s", new BotCommand.Alias(commands.get("rechercher")));
		commands.put("search", new BotCommand.Alias(this, "search", commands.get("rechercher")));
				
		commands.put("lib??rer", new BotCommand.SearchCommand(this, "lib??rer", "Retire une marque d???int??r??t.", ecritOption.setRequired(true), idOption) {
			
			@Override
			public void process(Ecrit e, CritiBot bot, MessageReceivedEvent message, String[] args) {
				archiver();
				if(e.getStatus() != Status.OUVERT_PLUS) {
					message.getChannel().sendMessage("????" + e.getNom() + "???? n'a aucune marque d'int??r??t.").queue();
					return;
				}
				if(e.liberer(message.getMember())) {
					message.getChannel().sendMessage("Marque d'int??r??t sur ????" + e.getNom() + "???? supprim??e.").queue();
				} else {
					message.getChannel().sendMessage("Vous n'avez pas de marque d'int??r??t sur ????" + e.getNom() + "????.").queue();
				}
			}

			@Override
			public void processSlash(Ecrit e, CritiBot bot, SlashCommandInteractionEvent event) {
				archiver();
				if(e.getStatus() != Status.OUVERT_PLUS) {
					event.reply("????" + e.getNom() + "???? n'a aucune marque d'int??r??t.").queue();
					return;
				}
				if(e.liberer(event.getMember())) {
					event.reply("Marque d'int??r??t sur ????" + e.getNom() + "???? supprim??e.").queue();
				} else {
					event.reply("Vous n'avez pas de marque d'int??r??t sur ????" + e.getNom() + "????.").queue();
				}
				
			}
		});
		
		commands.put("marquer", new BotCommand.SearchCommand(this, "marquer", "Marque d???int??r??t un ??crit.", ecritOption.setRequired(true), idOption) {

			@Override
			public void processSlash(Ecrit e, CritiBot bot, SlashCommandInteractionEvent event) {
				event.reply("Choisissez le type de votre marque.").addActionRow(InteretType.actionRow(e)).setEphemeral(true).queue();
			}
			
			@Override
			public void process(Ecrit e, CritiBot bot, MessageReceivedEvent message, String[] args) {
				message.getChannel().sendMessage("Cette commande n???est pas disponible en commande textuelle. Utilisez /editer_marque plut??t.").queue();
				
			}
		});
		
		commands.put("m", new BotCommand.Alias(commands.get("marquer")));
		
		commands.put("nettoyer", new BotCommand(this, "nettoyer", "Supprime les ??crits abandonn??s, publi??s et refus??s.") {
			
			@Override
			public void execute(CritiBot bot, MessageReceivedEvent message, String[] args) {
				archiver();
				bot.clean(false);
				message.getChannel().sendMessage("??crits abandonn??s, publi??s et refus??s supprim??s de la liste??!").queue();
			}

			@Override
			public void slash(CritiBot bot, SlashCommandInteractionEvent event) {
				archiver();
				bot.clean(false);
				event.reply("??crits abandonn??s, publi??s et refus??s supprim??s de la liste??!").queue();
				
			}
		});
		
		commands.put("statut", new BotCommand.SearchCommand(this, "statut", "Change le statut d???un ??crit", ecritOption.setRequired(true), statutOption.setRequired(true), idOption) {
			@Override
			public void process(Ecrit e, CritiBot bot, MessageReceivedEvent message, String[] args) {
				archiver();
				Status status = Status.getStatus(args[1]);
				boolean ret = e.setStatus(status);
				if(!ret) {
					message.getChannel().sendMessage("Ce statut ne peut ??tre assign?? manuellement.").queue();
				} else
					message.getChannel().sendMessage("Statut de ????" + e.getNom() + "???? chang????!").queue();
			}

			@Override
			public void processSlash(Ecrit e, CritiBot bot, SlashCommandInteractionEvent event) {
				archiver();
				Status status = Status.getStatus(event.getOption("statut").getAsString());
				boolean ret = e.setStatus(status);
				if(!ret) {
					event.reply("Ce statut ne peut ??tre assign?? manuellement.").queue();
				} else
					event.reply("Statut de ????" + e.getNom() + "???? chang????!").queue();
				
			}
		});
		
		commands.put("status", new BotCommand.Alias(commands.get("statut")));
		
		commands.put("type", new BotCommand.SearchCommand(this, "type", "Change le type d???un ??crit", ecritOption.setRequired(true), typeOption.setRequired(true), idOption) {
			
			@Override
			public void process(Ecrit e, CritiBot bot, MessageReceivedEvent message, String[] args) {
				archiver();
				Type status = Type.getType(args[1]);
				e.setType(status);
				message.getChannel().sendMessage("Type de ????" + e.getNom() + "???? chang????!").queue();
			}

			@Override
			public void processSlash(Ecrit e, CritiBot bot, SlashCommandInteractionEvent event) {
				archiver();
				Type status = Type.getType(event.getOption("type").getAsString());
				e.setType(status);
				event.reply("Type de ????" + e.getNom() + "???? chang????!").queue();
				
			}
		});
		
		commands.put("valider", new BotCommand.SearchCommand(this, "valider", "Valide un ??crit.", ecritOption.setRequired(true), idOption) {
			
			@Override
			public void process(Ecrit e, CritiBot bot, MessageReceivedEvent message, String[] args) {
				message.getMessage().delete().queue();
				archiver();
				e.promote();
				message.getChannel().sendMessage("Si l'??crit ????" + e.getNom() + "???? ??tait une id??e, c'est maintenant un rapport??! Sinon, rien n'a chang??.").queue();
				e.setStatus(Status.EN_ATTENTE);
				message.getChannel().sendMessage("L'??crit ????" + e.getNom() + "???? a ??t?? not?? comme critiqu????!").queue();
				
			}

			@Override
			public void processSlash(Ecrit e, CritiBot bot, SlashCommandInteractionEvent event) {
				archiver();
				e.promote();
				event.reply("Si l'??crit ????" + e.getNom() + "???? ??tait une id??e, c'est maintenant un rapport??! Sinon, rien n'a chang??.\nL'??crit ????" + e.getNom() + "???? a ??t?? not?? comme critiqu????!").queue();
				e.setStatus(Status.EN_ATTENTE);				
			}
		});
		
		commands.put("refuser", new BotCommand.SearchCommand(this, "refuser", "Refuse un ??crit.", ecritOption.setRequired(true), idOption) {
			
			@Override
			public void process(Ecrit e, CritiBot bot, MessageReceivedEvent message, String[] args) {
				e.setStatus(Status.REFUSE);
				message.getChannel().sendMessage("L'??crit ????" + e.getNom() + "???? a ??t?? refus??.").queue();
				
			}

			@Override
			public void processSlash(Ecrit e, CritiBot bot, SlashCommandInteractionEvent event) {
				e.setStatus(Status.REFUSE);
				event.reply("L'??crit ????" + e.getNom() + "???? a ??t?? refus??.").queue();
				
			}
		});
		
		commands.put("refus??", new BotCommand.Alias(commands.get("refuser")));
		
		commands.put("supprimer", new BotCommand.SearchCommand(this, "supprimer", "Supprime un ??crit.", ecritOption.setRequired(true), idOption) {
			
			@Override
			public void process(Ecrit e, CritiBot bot, MessageReceivedEvent message, String[] args) {
				archiver();
				bot.remove(e);
				message.getChannel().sendMessage("L'??crit ????" + e.getNom() + "???? a ??t?? supprim??.").queue();	
			}

			@Override
			public void processSlash(Ecrit e, CritiBot bot, SlashCommandInteractionEvent event) {
				archiver();
				bot.remove(e);
				event.reply("L'??crit ????" + e.getNom() + "???? a ??t?? supprim??.").queue();	
				
			}
		});
		
		commands.put("marquer_pour", new BotCommand.SearchCommand(this, "marquer_pour", "Marque d???int??r??t un ??crit pour quelqu???un d???autre.", ecritOption.setRequired(true),
				new OptionData(OptionType.STRING, "marque", "?? qui donner la marque").setRequired(true), new OptionData(OptionType.STRING, "type-marque", "Type de Marque").addChoices(choicesMarques), idOption) {
			
			@Override
			public void process(Ecrit e, CritiBot bot, MessageReceivedEvent message, String[] args) {
				if(args.length < 2) {
					message.getChannel().sendMessage("Il manque un argument. Utilisation??: c!marquer_pour {Crit??re};{Utilisateur}").queue();
					return;
				}
				archiver();
				if(e.marquer(args[1], InteretType.INSTANT)) {
					message.getChannel().sendMessage("????" + e.getNom() + "???? marqu?? d'int??ret pour " + args[1] + "??!").queue();
				} else {
					message.getAuthor().openPrivateChannel().complete().sendMessage("L'??crit ????" + e.getNom() + "???? ne peut pas ??tre marqu?? d'int??r??t car il n'est pas ouvert.").queue();
				}
				
			}

			@Override
			public void processSlash(Ecrit e, CritiBot bot, SlashCommandInteractionEvent event) {
				archiver();
				OptionMapping ito = event.getOption("type-marque");
				InteretType it;
				if(ito == null) {
					it = InteretType.INSTANT;
				} else {
					it = InteretType.getInteretType(ito.getAsString());
				}
				if(e.marquer(event.getOption("marque").getAsString(), it)) {
					event.reply("????" + e.getNom() + "???? marqu?? d'int??ret pour " + event.getOption("marque").getAsString() + "??!").queue();
				} else {
					event.reply("L'??crit ????" + e.getNom() + "???? ne peut pas ??tre marqu?? d'int??r??t car il n'est pas ouvert.").queue();
				}
				
			}
		});
		
		commands.put("lib??rer_pour", new BotCommand.SearchCommand(this, "lib??rer_pour", "Lib??re une marque d???int??r??t pour quelqu???un.", ecritOption.setRequired(true),
				new OptionData(OptionType.STRING, "marque", "De qui retirer la marque").setRequired(true), idOption) {
			
			@Override
			public void process(Ecrit e, CritiBot bot, MessageReceivedEvent message, String[] args) {
				if(args.length < 2) {
					message.getChannel().sendMessage("Il manque un argument. Utilisation??: c!lib??rer_pour {Crit??re};{Utilisateur}").queue();
					return;
				}
				archiver();
				if(e.getStatus() != Status.OUVERT_PLUS) {
					message.getChannel().sendMessage("????" + e.getNom() + "???? n'a aucune marque d'int??r??t.").queue();
					return;
				}
				if(e.liberer(args[1])) {
					message.getChannel().sendMessage("Marque d'int??r??t de " + args[1] + " sur ????" + e.getNom() + "???? supprim??e.").queue();
				} else {
					message.getChannel().sendMessage(args[1] + " n'a pas de marque d'int??r??t sur ????" + e.getNom() + "????.").queue();
				}
			}

			@Override
			public void processSlash(Ecrit e, CritiBot bot, SlashCommandInteractionEvent event) {
				archiver();
				if(e.getStatus() != Status.OUVERT_PLUS) {
					event.reply("????" + e.getNom() + "???? n'a aucune marque d'int??r??t.").queue();
					return;
				}
				if(e.liberer(event.getOption("marque").getAsString())) {
					event.reply("Marque d'int??r??t de " + event.getOption("marque").getAsString() + " sur ????" + e.getNom() + "???? supprim??e.").queue();
				} else {
					event.reply(event.getOption("marque").getAsString() + " n'a pas de marque d'int??r??t sur ????" + e.getNom() + "????.").queue();
				}
				
			}
		});
		
		commands.put("liberer_pour", new BotCommand.Alias(commands.get("lib??rer_pour")));
		
		commands.put("critiqu??", new BotCommand.SearchCommand(this, "critiqu??", "Indique qu???un ??crit ?? ??t?? critiqu??.", ecritOption.setRequired(true), idOption) {
			
			@Override
			public void process(Ecrit e, CritiBot bot, MessageReceivedEvent message, String[] args) {
				message.getMessage().delete().queue();
				archiver();
				message.getChannel().sendMessage("????" + e.getNom() + "???? critiqu????!").queue();
				e.critique();
			}

			@Override
			public void processSlash(Ecrit e, CritiBot bot, SlashCommandInteractionEvent event) {
				archiver();
				event.reply("????" + e.getNom() + "???? critiqu????!").queue();
				e.critique();	
			}
		});
		
		commands.put("critique", new BotCommand.Alias(commands.get("critiqu??")));
		
		commands.put("inbox", new BotCommand(this, "inbox", "R??cup??re les ??crits incorrectement balis??s.") {

			@Override
			public void execute(CritiBot bot, MessageReceivedEvent message, String[] args) {
				String mes = "Bo??te de r??ception??:\n";
				for(SyndEntry e : bot.getInbox()) {
					mes += e.getTitle() + " ??? " + e.getLink() + "\n";
				}
				sendLongMessage(mes, message.getChannel());
				bot.getInbox().clear();
				bot.getJda().getPresence().setStatus(OnlineStatus.ONLINE);
				
			}

			@Override
			public void slash(CritiBot bot, SlashCommandInteractionEvent event) {
				String mes = "Bo??te de r??ception??:\n";
				for(SyndEntry e : bot.getInbox()) {
					mes += e.getTitle() + " ??? " + e.getLink() + "\n";
				}
				event.reply(mes).queue();
				bot.getInbox().clear();
				bot.getJda().getPresence().setStatus(OnlineStatus.ONLINE);
				
			}
		});
		
		commands.put("archiver_avant", new BotCommand(this, "archiver_avant", "Marque ?????Sans nouvelle????? les ??crits plus vieux que la date indiqu??e.", dateOption.setRequired(true)) {

			@Override
			public void execute(CritiBot bot, MessageReceivedEvent message, String[] args) {
				archiver();
				long date = 0L;
				try {
					date = new SimpleDateFormat("dd/MM/yyyy").parse(args[0]).getTime();
					int n = 0;
					for(Ecrit e : bot.getEcrits()) {
						if(e.olderThan(date) && !e.isDead()) {
							e.setStatus(Status.SANS_NOUVELLES);
							n++;
						}
					}
					message.getChannel().sendMessage(n + " ??crits ont ??t?? marqu??s sans nouvelles depuis le " + new SimpleDateFormat("dd MMM yyyy").format(new Date(date)) + ".").queue();
				} catch(ParseException | NullPointerException e) {
					message.getChannel().sendMessage("Erreur dans le format de la date.").queue();
				}
				
			}

			@Override
			public void slash(CritiBot bot, SlashCommandInteractionEvent event) {
				archiver();
				long date = 0L;
				try {
					date = new SimpleDateFormat("dd/MM/yyyy").parse(event.getOption("date").getAsString()).getTime();
					int n = 0;
					for(Ecrit e : bot.getEcrits()) {
						if(e.olderThan(date) && !e.isDead()) {
							e.setStatus(Status.SANS_NOUVELLES);
							n++;
						}
					}
					event.reply(n + " ??crits ont ??t?? marqu??s sans nouvelles depuis le " + new SimpleDateFormat("dd MMM yyyy").format(new Date(date)) + ".").queue();
				} catch(ParseException | NullPointerException e) {
					event.reply("Erreur dans le format de la date.").queue();
				}
				
			}
			
		});
		
		commands.put("annuler", new BotCommand(this, "annuler", "Annule la derni??re commande.") {

			@Override
			public void execute(CritiBot bot, MessageReceivedEvent message, String[] args) {
				if(!bot.annuler()) {
					message.getChannel().sendMessage("Aucune modification effectu??e.").queue();
				} else {
					message.getChannel().sendMessage("Derni??re modification annul??e??!").queue();
				}
				
			}

			@Override
			public void slash(CritiBot bot, SlashCommandInteractionEvent event) {
				if(!bot.annuler()) {
					event.reply("Aucune modification effectu??e.").queue();
				} else {
					event.reply("Derni??re modification annul??e??!").queue();
				}
				
			}
			
		});
		
		commands.put("update_open", new BotCommand(this, "update_open", "V??rifie que les salons d?????crits sont bien ?? jour.") {

			@Override
			public void execute(CritiBot bot, MessageReceivedEvent message, String[] args) {
				bot.updateOpen();
				message.getChannel().sendMessage("Salons d?????crits mis ?? jour.").queue();
			}

			@Override
			public void slash(CritiBot bot, SlashCommandInteractionEvent event) {
				bot.updateOpen();
				event.reply("Salons d?????crits mis ?? jour.").queue();
				
			}
			
		});
		
		commands.put("refresh_messages", new BotCommand(this, "refresh_messages", "R??initialise les salons d?????crits.") {

			@Override
			public void execute(CritiBot bot, MessageReceivedEvent message, String[] args) {
				bot.refreshMessages();
				message.getChannel().sendMessage("Salons d?????crits r??initialis??s.").queue();
			}

			@Override
			public void slash(CritiBot bot, SlashCommandInteractionEvent event) {
				bot.refreshMessages();
				event.reply("Salons d?????crits r??initialis??s.").queue();
				
			}
			
		});
		
		commands.put("maj", new BotCommand(this, "maj", "Met ?? jour la BDD avec les nouveaux ??crits.") {

			@Override
			public void execute(CritiBot bot, MessageReceivedEvent message, String[] args) {
				try {
					int tailleAncienne = bot.getEcrits().size();
					addNew();
					if(tailleAncienne != bot.getEcrits().size()) {
						bot.updateOpen();
					}
					message.getChannel().sendMessage("Mise ?? jour effectu??e.").queue();
				} catch (IllegalArgumentException | FeedException | IOException | NullPointerException e) {
					jda.getTextChannelById(737725144390172714L).sendMessage("<@340877529973784586>\n" + e.getLocalizedMessage()).queue();
					e.printStackTrace();
				}
				
			}

			@Override
			public void slash(CritiBot bot, SlashCommandInteractionEvent event) {
				try {
					int tailleAncienne = bot.getEcrits().size();
					addNew();
					if(tailleAncienne != bot.getEcrits().size()) {
						bot.updateOpen();
					}
					event.reply("Mise ?? jour effectu??e.").queue();
				} catch (IllegalArgumentException | FeedException | IOException | NullPointerException e) {
					jda.getTextChannelById(737725144390172714L).sendMessage("<@340877529973784586>\n" + e.getLocalizedMessage()).queue();
					e.printStackTrace();
					event.reply("Une erreur est survenue.").queue();
				}
				
			}
			
		});
		
		commands.put("renommer", new BotCommand.SearchCommand(this, "renommer", "Renomme un ??crit", ecritOption.setRequired(true),
				new OptionData(OptionType.STRING, "nouveau_nom", "Nouveau nom de l?????crit.").setRequired(true), idOption) {
			
			@Override
			public void process(Ecrit e, CritiBot bot, MessageReceivedEvent message, String[] args) {
				archiver();
				String oldName = e.getNom();
				try {
					e.rename(args[1]);
					message.getChannel().sendMessage("????" + oldName + "???? renomm?? en ????" + e.getNom() + "????.").queue();
				} catch(ArrayIndexOutOfBoundsException ex) {
					message.getChannel().sendMessage("Utilisation??: `c!rename {Crit??re};{Nouveau nom}`").queue();
				}
			}

			@Override
			public void processSlash(Ecrit e, CritiBot bot, SlashCommandInteractionEvent event) {
				archiver();
				String oldName = e.getNom();
				e.rename(event.getOption("nouveau_nom").getAsString());
				event.reply("????" + oldName + "???? renomm?? en ????" + e.getNom() + "????.").queue();
				
			}
		});
		
		commands.put("doublons", new BotCommand(this, "doublons", "Nettoie les doublons de la base de donn??es.") {

			@Override
			public void execute(CritiBot bot, MessageReceivedEvent message, String[] args) {
				archiver();
				bot.doublons();
				message.getChannel().sendMessage("Doublons supprim??s??!").queue();
			}

			@Override
			public void slash(CritiBot bot, SlashCommandInteractionEvent event) {
				archiver();
				bot.doublons();
				event.reply("Doublons supprim??s??!").queue();
			}
			
		});
		
		commands.put("up", new BotCommand.SearchCommand(this, "up", "Remet un ??crit ouvert et le remet en dernier message du salon.", ecritOption.setRequired(true), idOption) {
			
			@Override
			public void process(Ecrit e, CritiBot bot, MessageReceivedEvent message, String[] args) {
				for(Affichan aff : affichans) {
					aff.up(e);
				}
				if(e.getStatus() != Status.OUVERT_PLUS) {
					e.setStatus(Status.OUVERT);
				}
				message.getChannel().sendMessage("????" + e.getNom() + "???? up??!").queue();
			}

			@Override
			public void processSlash(Ecrit e, CritiBot bot, SlashCommandInteractionEvent event) {
				for(Affichan aff : affichans) {
					aff.up(e);
				}
				if(e.getStatus() != Status.OUVERT_PLUS) {
					e.setStatus(Status.OUVERT);
				}
				event.reply("????" + e.getNom() + "???? up??!").queue();
				
			}
		});
		
		commands.put("r??ouvert", new BotCommand.Alias(this, "r??ouvert", commands.get("up")));
		commands.put("ouvrir", new BotCommand.Alias(this, "ouvrir", commands.get("up")));
		commands.put("o", new BotCommand.Alias(commands.get("up")));
		commands.put("reouvert", new BotCommand.Alias(commands.get("up")));
		
		commands.put("manual_update", new BotCommand(this, "manual_update", "Indique le bot comme mis ?? jour.") {

			@Override
			public void execute(CritiBot bot, MessageReceivedEvent message, String[] args) {
				bot.lastUpdate = System.currentTimeMillis();
				bot.getJda().getPresence().setActivity(Activity.playing("critiquer. Derni??re mise ?? jour forum??le " + new SimpleDateFormat("dd MMM??yyyy ?? HH:mm").format(new Date(lastUpdate))));
				message.getChannel().sendMessage("Bot indiqu?? comme mis ?? jour.").queue();
				
			}

			@Override
			public void slash(CritiBot bot, SlashCommandInteractionEvent event) {
				bot.lastUpdate = System.currentTimeMillis();
				bot.getJda().getPresence().setActivity(Activity.playing("critiquer. Derni??re mise ?? jour forum??le " + new SimpleDateFormat("dd MMM??yyyy ?? HH:mm").format(new Date(lastUpdate))));
				event.reply("Bot indiqu?? comme mis ?? jour.").queue();
				
			}
			
		});
		
		commands.put("bdd", new BotCommand(this, "bdd", "Renvoie la base de donn??es.") {

			@Override
			public void execute(CritiBot bot, MessageReceivedEvent message, String[] args) {
				try {
					bot.save();
					message.getChannel().sendFile(new File("/home/cyrielle/bots/critibot.json")).queue();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					message.getChannel().sendMessage("Impossible de sauvegarder la base de donn??es en premier lieu.").queue();
				}
				
				
			}

			@Override
			public void slash(CritiBot bot, SlashCommandInteractionEvent event) {
				try {
					bot.save();
					event.replyFile(new File("/home/cyrielle/bots/critibot.json")).queue();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					event.reply("Impossible de sauvegarder la base de donn??es en premier lieu.").queue();
				}
				
			}
			
		});
		
		commands.put("auteur", new BotCommand.SearchCommand(this, "auteur", "Change l???auteur d???un ??crit.", ecritOption.setRequired(true),
				new OptionData(OptionType.STRING, "auteur", "Auteur de l?????crit").setRequired(true), idOption) {

			@Override
			public void process(Ecrit e, CritiBot bot, MessageReceivedEvent message, String[] args) {
				archiver();
				try {
					e.setAuteur(args[1]);
					message.getChannel().sendMessage("????" + args[1] + "???? a ??t?? d??fini comme l'auteur(ice) de ????" + e.getNom() + "????.").queue();
				} catch(ArrayIndexOutOfBoundsException ex) {
					message.getChannel().sendMessage("Utilisation??: `c!auteur {Crit??re};{Auteur}`").queue();
				}
				
			}

			@Override
			public void processSlash(Ecrit e, CritiBot bot, SlashCommandInteractionEvent event) {
				archiver();
				e.setAuteur(event.getOption("auteur").getAsString());
				event.reply("????" + event.getOption("auteur").getAsString() + "???? a ??t?? d??fini comme l'auteur(ice) de ????" + e.getNom() + "????.").queue();
				
			}
			
		});
		
		commands.put("taille_bdd", new BotCommand(this, "taille_bdd", "Renvoie la taille de la base de donn??es.") {

			@Override
			public void execute(CritiBot bot, MessageReceivedEvent message, String[] args) {
				message.getChannel().sendMessage("Il y a actuellement " + bot.getEcrits().size() + " ??crits dans la base de donn??es.").queue();
			}

			@Override
			public void slash(CritiBot bot, SlashCommandInteractionEvent event) {
				event.reply("Il y a actuellement " + bot.getEcrits().size() + " ??crits dans la base de donn??es.").queue();
				
			}
			
		});
		
		commands.put("ulister", new BotCommand(this, "ulister", "Liste les ??crits avec des crit??res pr??cis.",
				new OptionData(OptionType.STRING, "nom", "Crit??re de nom des ??crits"),
				new OptionData(OptionType.STRING, "statuts", "Statuts recherch??s, s??par??s par des virgules"),
				new OptionData(OptionType.STRING, "types", "Types recherch??s, s??par??s par des virgules"),
				new OptionData(OptionType.STRING, "auteurs", "Auteurs recherch??s, s??par??s par des virgules"),
				new OptionData(OptionType.STRING, "tags", "Tags recherch??s, s??par??s par des virgules"),
				new OptionData(OptionType.BOOLEAN, "tag-inclusif", "Vrai si l?????crit doit poss??der tous les tags, Faux si l???un d???entre eux suffit"),
				new OptionData(OptionType.STRING, "modifie-avant", "Date maximale de derni??re modification de l?????crit (jj/mm/aaaa)"),
				new OptionData(OptionType.STRING, "modifie-apres", "Date minimale de derni??re modification de l?????crit (jj/mm/aaaa)")){
			
			public Vector<MessageEmbed> search(String critere, Vector<Status> status, Vector<Type> type, Vector<String> authors, Vector<String> tags, boolean tagAnd, long modAvant, long modApres) {
				
				
				Vector<Ecrit> choisis = ulister(critere, status, type, authors, tags, tagAnd, modAvant, modApres);
				
				// Vector des messages ?? envoyer.
				Vector<MessageEmbed> embeds = new Vector<MessageEmbed>();
				// Vector du contenu des embeds.
				Vector<String> messages = new Vector<String>();
				// S??pare les r??sultats de la recherche pour que chaque message n'exc??de pas les 2??000 caract??res.
				String buffer = "";
				for(Ecrit e : sortByDate(choisis)) {
					// Remplit d'abord un buffer puis lorsqu'il est trop grand, ajoute son contenu dans ????messages???? et le vide.
					String toAdd = "[**" + e.getNom() + "**](" + e.getLien() + ")\n" + e.getAuteur() + "\n" + e.getStatus() + " ??? " + e.getType() + "\n\n";
					if(buffer.length() + toAdd.length() > 1000) {
						messages.add(buffer);
						buffer = "";
					}
					buffer += toAdd;
				}
				if(buffer.isEmpty()) { // Si le buffer est vide, c'est qu'il n'a jamais ??t?? rempli??: aucun r??sultat, donc.
					EmbedBuilder b = new EmbedBuilder();
					b.setTitle("Aucun r??sultat");
					b.setAuthor("Recherche??personnalis??e");
					b.setTimestamp(Instant.now());
					b.setColor(16001600);
					embeds.add(b.build());
				} else { // Sinon, envoie les r??sultats.
					messages.add(buffer); // Ajoute le buffer restant aux messages.
					EmbedBuilder b = new EmbedBuilder();
					b.setTitle("R??sultats de la recherche");
					b.setAuthor("Recherche??personnalis??e");
					b.setTimestamp(Instant.now());
					b.setColor(73887);
					for(int i = 0; i < messages.size(); i++) { // Cr??e les diff??rents messages ?? envoyer en num??rotant les pages.
						b.setFooter("Page " + (i + 1) + "/" + messages.size());
						b.setDescription(messages.get(i));
						embeds.add(b.build());
					}
				}
				
				return embeds;
				
			}
			
			@Override
			public void execute(CritiBot bot, MessageReceivedEvent message, String[] args) {
				// Liste de tous les param??tres possibles
				String critere = "";
				Vector<Status> status = new Vector<Status>();
				Vector<Type> type = new Vector<Type>();
				Vector<String> authors = new Vector<String>();
				Vector<String> tags = new Vector<String>();
				boolean tagAnd = false;
				long modAvant = 0;
				long modApres = 0;

				// R??cup??ration des param??tres
				for(int i = 0; i < args.length; i++) {
					String[] c = args[i].split("=");
					if(c.length != 2) {
						message.getChannel().sendMessage("Erreur de syntaxe dans le param??tre " + (i+1) + " : le param??tre n'est pas s??parables en deux parties par un symbole ????=????.").queue();
						return;
					}
					String npara = basicize(c[0]);
					if(npara.equals("critere") || npara.equals("nom")) {
						critere = c[1];
					} else if(npara.startsWith("statut") || npara.equals("status")) {
						String[] st = c[1].split(",");
						for(String s : st) {
							status.add(Status.getStatus(s));
						}
						
					} else if(npara.startsWith("type")) {
						String[] ty = c[1].split(",");
						for(String t : ty) {
							type.add(Type.getType(t));
						}
					} else if(npara.startsWith("auteur") || npara.startsWith("autrice")) {
						String[] aut = c[1].split(",");
						String errMes = "";
						for(String au : aut) {
							Vector<String> found = autSearch(au);
							if(!found.isEmpty())
								for(String a : found) {
									authors.add(a);
								}
							else
								errMes += "Auteur ????" + au + "???? non trouv??.\n";
						}
						if(aut.length != 0 && authors.isEmpty()) {
							errMes += "Aucun auteur de la liste trouv????; annulation.";
						}
						if(errMes != "") {
							message.getChannel().sendMessage(errMes).queue();
							return;
						}
					} else if(npara.startsWith("tag")) {
						String[] ta = c[1].split(",");
						for(String t : ta) {
							tags.add(t);
						}
						tagAnd = npara.contains("&");
					} else if(npara.equals("avant")) {
						try {
							modAvant = new SimpleDateFormat("dd/MM/yyyy").parse(args[0]).getTime();
						} catch (ParseException e) {
							message.getChannel().sendMessage("Erreur de syntaxe dans le param??tre " + (i+1) + "??: la date doit ??tre au format jj/mm/aaaa.").queue();
							return;
						}
					} else if(npara.equals("apres")) {
						try {
							modApres = new SimpleDateFormat("dd/MM/yyyy").parse(args[0]).getTime();
						} catch (ParseException e) {
							message.getChannel().sendMessage("Erreur de syntaxe dans le param??tre " + (i+1) + "??: la date doit ??tre au format jj/mm/aaaa.").queue();
							return;
						}
					} else {
						message.getChannel().sendMessage("Erreur de syntaxe dans le param??tre " + (i+1) + "??: param??tre ????" + c[0] + "???? inconnu.").queue();
						return;
					}
				}
				
				sendEmbeds(message.getChannel(), search(critere, status, type, authors, tags, tagAnd, modAvant, modApres));
				
			}

			@Override
			public void slash(CritiBot bot, SlashCommandInteractionEvent event) {
				// Liste de tous les param??tres possibles
				String critere = "";
				Vector<Status> status = new Vector<Status>();
				Vector<Type> type = new Vector<Type>();
				Vector<String> authors = new Vector<String>();
				Vector<String> tags = new Vector<String>();
				boolean tagAnd = false;
				long modAvant = 0;
				long modApres = 0;
				
				OptionMapping critereOpt = event.getOption("nom");
				OptionMapping statusOpt = event.getOption("statuts");
				OptionMapping typeOpt = event.getOption("types");
				OptionMapping authorsOpt = event.getOption("auteurs");
				OptionMapping tagsOpt = event.getOption("tags");
				OptionMapping tagAndOpt = event.getOption("tag-inclusif");
				OptionMapping modAvantOpt = event.getOption("modifie-avant");
				OptionMapping modApresOpt = event.getOption("modifie-apres");
				
				if(critereOpt == null && statusOpt == null && typeOpt == null && authorsOpt == null &&
						tagsOpt == null && tagAndOpt == null && modAvantOpt == null && modApresOpt == null) {
					event.reply("Il faut au moins un param??tre non-nul.").queue();
					return;
				}
				
				if(critereOpt != null) {
					critere = basicize(critereOpt.getAsString());
				}
				if(statusOpt != null) {
					String[] st = basicize(statusOpt.getAsString()).split(",");
					for(String s : st) {
						status.add(Status.getStatus(s));
					}
				}
				if(typeOpt != null) {
					String[] ty = basicize(typeOpt.getAsString()).split(",");
					for(String t : ty) {
						type.add(Type.getType(t));
					}
				}
				if(authorsOpt != null) {
					String[] aut = basicize(authorsOpt.getAsString()).split(",");
					String errMes = "";
					for(String au : aut) {
						Vector<String> found = autSearch(au);
						if(!found.isEmpty())
							for(String a : found) {
								authors.add(a);
							}
						else
							errMes += "Auteur ????" + au + "???? non trouv??.\n";
					}
					if(aut.length != 0 && authors.isEmpty()) {
						errMes += "Aucun auteur de la liste trouv????; annulation.";
					}
					if(errMes != "") {
						event.reply(errMes).queue();
						return;
					}
				
				}
				if(tagsOpt != null) {
					String[] ta = basicize(tagsOpt.getAsString()).split(",");
					for(String t : ta) {
						tags.add(t);
					}
				}
				if(tagAndOpt != null) {
					tagAnd = tagAndOpt.getAsBoolean();
				}
				if(modAvantOpt != null) {
					try {
						modAvant = new SimpleDateFormat("dd/MM/yyyy").parse(modAvantOpt.getAsString()).getTime();
					} catch (ParseException e) {
						event.reply("Erreur de syntaxe dans le param??tre modifie-avant??: la date doit ??tre au format jj/mm/aaaa.").queue();
						return;
					}
				}
				if(modApresOpt != null) {
					try {
						modApres = new SimpleDateFormat("dd/MM/yyyy").parse(modApresOpt.getAsString()).getTime();
					} catch (ParseException e) {
						event.reply("Erreur de syntaxe dans le param??tre modifie-apres??: la date doit ??tre au format jj/mm/aaaa.").queue();
						return;
					}
				}
				
				sendEmbeds(event, search(critere, status, type, authors, tags, tagAnd, modAvant, modApres));
			}
		});
		
		commands.put("ul", new BotCommand.Alias(commands.get("ulister")));
		
		commands.put("ajouter_tag", new BotCommand.SearchCommand(this, "ajouter_tag", "Ajoute un tag ?? l?????crit", ecritOption.setRequired(true),
				new OptionData(OptionType.STRING, "tag", "Tag ?? ajouter").setRequired(true), idOption) {
			
			@Override
			public void process(Ecrit e, CritiBot bot, MessageReceivedEvent message, String[] args) {
				archiver();
				if(e.addTag(args[1])) {
					message.getChannel().sendMessage("Tag ????" + args[1] + "???? ajout?? ?? l'??crit ????" + e.getNom() + "????.").queue();
				} else {
					message.getChannel().sendMessage("Ce tag est d??j?? ajout?? ?? l'??crit ????" + e.getNom() + "????.").queue();
				}
			}

			@Override
			public void processSlash(Ecrit e, CritiBot bot, SlashCommandInteractionEvent event) {
				archiver();
				if(e.addTag(event.getOption("tag").getAsString())) {
					event.reply("Tag ????" + event.getOption("tag").getAsString() + "???? ajout?? ?? l'??crit ????" + e.getNom() + "????.").queue();
				} else {
					event.reply("Ce tag est d??j?? ajout?? ?? l'??crit ????" + e.getNom() + "????.").queue();
				}
				
			}
		});
		
		commands.put("atag", new BotCommand.Alias(this, "atag", commands.get("ajouter_tag")));
		
		commands.put("retirer_tag", new BotCommand.SearchCommand(this, "retirer_tag", "Retire un tag d???un ??crit.",  ecritOption.setRequired(true),
				new OptionData(OptionType.STRING, "tag", "Tag ?? retirer.").setRequired(true), idOption) {
			
			@Override
			public void process(Ecrit e, CritiBot bot, MessageReceivedEvent message, String[] args) {
				archiver();
				if(e.removeTag(args[1])) {
					message.getChannel().sendMessage("Tag correspondant au crit??re retir????de l'??crit ????" + e.getNom() + "????.").queue();
				} else {
					message.getChannel().sendMessage("Aucun tag correspondant au crit??re n'est assign?? ?? l'??crit ?? " + e.getNom() + "????, ou plus d'un tag de l'??crit correspondent au crit??re.").queue();
				}
				
			}

			@Override
			public void processSlash(Ecrit e, CritiBot bot, SlashCommandInteractionEvent event) {
				archiver();
				if(e.removeTag(event.getOption("tag").getAsString())) {
					event.reply("Tag correspondant au crit??re retir????de l'??crit ????" + e.getNom() + "????.").queue();
				} else {
					event.reply("Aucun tag correspondant au crit??re n'est assign?? ?? l'??crit ?? " + e.getNom() + "????, ou plus d'un tag de l'??crit correspondent au crit??re.").queue();
				}
				
			}
		});
		
		commands.put("supprimer_tag", new BotCommand.Alias(this, "supprimer_tag", commands.get("retirer_tag")));
		commands.put("rtag", new BotCommand.Alias(this, "rtag", commands.get("retirer_tag")));
		commands.put("stag", new BotCommand.Alias(this, "stag", commands.get("retirer_tag")));
		
		commands.put("lister_tags", new BotCommand(this, "lister_tags", "Liste tous les tags de la base de donn??es.") {

			public Vector<MessageEmbed> search(CritiBot bot) {
				Vector<String> tags = new Vector<String>();
				Vector<Integer> nbEcrits = new Vector<Integer>();
				for(Ecrit e : bot.ecrits) {
					for(String tag : e.getTags()) {
						int index = tags.indexOf(tag);
						if(index < 0) {
							index = tags.size();
							tags.add(tag);
							nbEcrits.add(0);
						}
						nbEcrits.set(index, nbEcrits.get(index) + 1);
					}
				}
				
				// Vector des messages ?? envoyer.
				Vector<MessageEmbed> embeds = new Vector<MessageEmbed>();
				// Vector du contenu des embeds.
				Vector<String> messages = new Vector<String>();
				// S??pare les r??sultats de la recherche pour que chaque message n'exc??de pas les 2??000 caract??res.
				String buffer = "";
				for(int i = 0; i < tags.size(); i++) {
					// Remplit d'abord un buffer puis lorsqu'il est trop grand, ajoute son contenu dans ????messages???? et le vide.
					String toAdd = "**" + tags.get(i) + "**\n" + nbEcrits.get(i).toString() + " ??crits\n\n";
					if(buffer.length() + toAdd.length() > 1000) {
						messages.add(buffer);
						buffer = "";
					}
					buffer += toAdd;
				}
				if(buffer.isEmpty()) { // Si le buffer est vide, c'est qu'il n'a jamais ??t?? rempli??: aucun r??sultat, donc.
					EmbedBuilder b = new EmbedBuilder();
					b.setTitle("Aucun tag dans la base de donn??es");
					b.setAuthor("Liste des tags");
					b.setTimestamp(Instant.now());
					b.setColor(16001600);
					embeds.add(b.build());
				} else { // Sinon, envoie les r??sultats.
					messages.add(buffer); // Ajoute le buffer restant aux messages.
					EmbedBuilder b = new EmbedBuilder();
					b.setTitle("Liste des tags");
					b.setAuthor("Liste des tags");
					b.setTimestamp(Instant.now());
					b.setColor(73887);
					for(int i = 0; i < messages.size(); i++) { // Cr??e les diff??rents messages ?? envoyer en num??rotant les pages.
						b.setFooter("Page " + (i + 1) + "/" + messages.size());
						b.setDescription(messages.get(i));
						embeds.add(b.build());
					}
				}
				return embeds;
			}
			
			@Override
			public void execute(CritiBot bot, MessageReceivedEvent message, String[] args) {
				sendEmbeds(message.getChannel(), search(bot));
				
			}

			@Override
			public void slash(CritiBot bot, SlashCommandInteractionEvent event) {
				sendEmbeds(event, search(bot));
				
			}
			
		});
		
		commands.put("majs_depuis", new BotCommand(this, "majs_depuis", "Renvoie tous les fils modifi??s depuis une date.", dateOption.setRequired(true)) {
			
			public Vector<MessageEmbed> search(String dateStr) throws ParseException {
				long date = 0L;
				date = new SimpleDateFormat("dd/MM/yyyy").parse(dateStr).getTime();
				
				try {
					Vector<Ecrit> ecritsMaj = new Vector<Ecrit>();
					Vector<String> neo = new Vector<String>();
					// R??cup??re le flux
					SyndFeed feed = new SyndFeedInput().build(new XmlReader(new URL("http://fondationscp.wikidot.com/feed/forum/cp-656675.xml")));
					for(Object o : feed.getEntries()) { // Pour chaque entr??e
						SyndEntry entry = (SyndEntry) o;
						if(entry.getPublishedDate().after(new Date(date))) { // Ne prend que les entr??es plus r??centes que la date indiqu??e
							int threadID = Ecrit.fIDtoInt(entry.getLink().substring(7).split("/")[2]); // R??cup??re l???ID du thread
							Ecrit ecr = null;
							for(Ecrit e : ecrits) { // Recherche l?????crit avec le m??me ID de thread
								int eID = e.hashCode();
								if(threadID == eID) { // Identifie l?????crit trouv?? ?? la mise ?? jour
									ecr = e;
									break;
								}
							}
							if(ecr != null && !ecritsMaj.contains(ecr)) {
								ecritsMaj.add(ecr);
							} else if(!neo.contains(entry.getLink()) && ecr == null) {
								neo.add(entry.getLink());
							}
						}
					}
					// Vector des messages ?? envoyer.
					Vector<MessageEmbed> embeds = new Vector<MessageEmbed>();
					// Vector du contenu des embeds.
					Vector<String> messages = new Vector<String>();
					// S??pare les r??sultats de la recherche pour que chaque message n'exc??de pas les 2??000 caract??res.
					String buffer = "";
					for(Ecrit e : sortByDate(ecritsMaj)) {
						// Remplit d'abord un buffer puis lorsqu'il est trop grand, ajoute son contenu dans ????messages???? et le vide.
						String toAdd = "[**" + e.getNom() + "**](" + e.getLien() + ")\n" + e.getAuteur() + "\n" + e.getStatus() + " ??? " + e.getType() + "\n\n";
						if(buffer.length() + toAdd.length() > 1000) {
							messages.add(buffer);
							buffer = "";
						}
						buffer += toAdd;
					}
					
					for(String str : neo) {
						String toAdd = str + "\n\n";
						if(buffer.length() + toAdd.length() > 1000) {
							messages.add(buffer);
							buffer = "";
						}
						buffer += toAdd;
					}
					if(buffer.isEmpty()) { // Si le buffer est vide, c'est qu'il n'a jamais ??t?? rempli??: aucun r??sultat, donc.
						EmbedBuilder b = new EmbedBuilder();
						b.setTitle("Aucun r??sultat");
						b.setAuthor("??crits mis ?? jour depuis le " + dateStr);
						b.setTimestamp(Instant.now());
						b.setColor(16001600);
						embeds.add(b.build());
					} else { // Sinon, envoie les r??sultats.
						messages.add(buffer); // Ajoute le buffer restant aux messages.
						EmbedBuilder b = new EmbedBuilder();
						b.setTitle("R??sultats de la recherche");
						b.setAuthor("??crits mis ?? jour depuis le " + dateStr);
						b.setTimestamp(Instant.now());
						b.setColor(73887);
						for(int i = 0; i < messages.size(); i++) { // Cr??e les diff??rents messages ?? envoyer en num??rotant les pages.
							b.setFooter("Page " + (i + 1) + "/" + messages.size());
							b.setDescription(messages.get(i));
							embeds.add(b.build());
						}
					}
					return embeds;
				} catch (IllegalArgumentException | FeedException | IOException e) {
					return null;
					
				}
			}
			 
			@Override
			public void execute(CritiBot bot, MessageReceivedEvent message, String[] args) {
				if(args.length == 0) {
					message.getChannel().sendMessage("Utilisation???: c!updates_from jj/mm/aaaa").queue();
					return;
				}
				try {
					Vector<MessageEmbed> embeds = search(args[0]);
					if(embeds == null)
						message.getChannel().sendMessage("Erreur lors de la r??cup??ration du flux.").queue();
					else
						sendEmbeds(message.getChannel(), embeds);
				} catch (ParseException e) {
					message.getChannel().sendMessage("Utilisation???: c!updates_from jj/mm/aaaa").queue();
				}
				
				
			}

			@Override
			public void slash(CritiBot bot, SlashCommandInteractionEvent event) {
				try {
					Vector<MessageEmbed> embeds = search(event.getOption("date").getAsString());
					if(embeds == null)
						event.reply("Erreur lors de la r??cup??ration du flux.").queue();
					else
						sendEmbeds(event, embeds);
				} catch (ParseException e) {
					event.reply("Utilisation???: c!updates_from jj/mm/aaaa").queue();
				}
				
			}
		});
		
		commands.put("updates_from", new BotCommand.Alias(this, "updates_from", commands.get("majs_depuis")));
		
		commands.put("al??atoire", new BotCommand(this, "al??atoire", "Renvoie un ??crit ouvert al??atoire (parmi les types indiqu??s si sp??cifi??s)", 
				new OptionData(OptionType.STRING, "types", "Types ?? s??l??ctionner, s??par??s par des virgules")) {

			@Override
			public void execute(CritiBot bot, MessageReceivedEvent message, String[] args) {
				Vector<Type> t = new Vector<Type>();
				if(args.length > 0) {
					for(String tstr : args) {
						t.add(Type.getType(tstr));
					}
				}
				Vector<Status> s = new Vector<Status>();
				s.add(Status.OUVERT);
				Vector<Ecrit> correspondants = ulister("", s, t, new Vector<String>(), new Vector<String>(), false, 0, 0);
				if(correspondants.size() == 0)
					message.getChannel().sendMessage("Aucun ??crit possible trouv??.").queue();
				else {
					Ecrit choisi = correspondants.get(random.nextInt(correspondants.size()));
					Affichan.sendMessageWithActions(choisi, message.getTextChannel()).queue();
				}
			}

			@Override
			public void slash(CritiBot bot, SlashCommandInteractionEvent event) {
				Vector<Type> t = new Vector<Type>();
				OptionMapping types = event.getOption("types");
				if(types != null)
					for(String tstr : types.getAsString().split(",")) {
						t.add(Type.getType(tstr));
					}

				Vector<Status> s = new Vector<Status>();
				s.add(Status.OUVERT);
				Vector<Ecrit> correspondants = ulister("", s, t, new Vector<String>(), new Vector<String>(), false, 0, 0);
				if(correspondants.size() == 0)
					event.reply("Aucun ??crit possible trouv??.").queue();
				else {
					Ecrit choisi = correspondants.get(random.nextInt(correspondants.size()));
					event.deferReply().queue();
					Affichan.sendMessageWithActions(choisi, event.getHook()).queue();
				}
				
			}
			
		});
		
		commands.put("random", new BotCommand.Alias(this, "random", commands.get("al??atoire")));
		commands.put("r", new BotCommand.Alias(commands.get("al??atoire")));
		
		commands.put("edit_all", new BotCommand(this, "edit_all", "Modifie et rafra??chit tous les messages dans les salons d???affichage.") {

			@Override
			public void execute(CritiBot bot, MessageReceivedEvent message, String[] args) {
				for(Affichan aff : affichans)
					aff.editAll();
				message.getChannel().sendMessage("Fait").queue();
			}

			@Override
			public void slash(CritiBot bot, SlashCommandInteractionEvent event) {
				for(Affichan aff : affichans)
					aff.editAll();
				event.reply("Fait").queue();
			}
			
		});
		
		commands.put("ancien", new BotCommand(this, "ancien", "Renvoie l?????crit ouvert le plus ancien (parmi les types s??l??ctionn??s si sp??cifi??s)",
				new OptionData(OptionType.STRING, "types", "Types ?? s??l??ctionner, s??par??s par des virgules")) {

			@Override
			public void execute(CritiBot bot, MessageReceivedEvent message, String[] args) {
				Vector<Type> t = new Vector<Type>();
				for(String tstr : args) {
					t.add(Type.getType(tstr));
				}
				Vector<Status> s = new Vector<Status>();
				s.add(Status.OUVERT);
				Vector<Ecrit> correspondants = ulister("", s, t, new Vector<String>(), new Vector<String>(), false, 0, 0);
				if(correspondants.size() == 0)
					message.getChannel().sendMessage("Aucun ??crit possible trouv??.").queue();
				else {
					Ecrit choisi = sortByDate(correspondants).get(0);
					Affichan.sendMessageWithActions(choisi, message.getTextChannel()).queue();
				}
			}

			@Override
			public void slash(CritiBot bot, SlashCommandInteractionEvent event) {
				Vector<Type> t = new Vector<Type>();
				OptionMapping types = event.getOption("types");
				if(types != null)
					for(String tstr : types.getAsString().split(",")) {
						t.add(Type.getType(tstr));
					}
				Vector<Status> s = new Vector<Status>();
				s.add(Status.OUVERT);
				Vector<Ecrit> correspondants = ulister("", s, t, new Vector<String>(), new Vector<String>(), false, 0, 0);
				if(correspondants.size() == 0)
					event.reply("Aucun ??crit possible trouv??.").queue();
				else {
					Ecrit choisi = sortByDate(correspondants).get(0);
					event.deferReply().queue();
					Affichan.sendMessageWithActions(choisi, event.getHook()).queue();
				}
				
			}
			
		});
		
		commands.put("a", new BotCommand.Alias(commands.get("ancien")));
	}
	
	/**
	 * Supprime les doublons de la liste.
	 */
	public void doublons() {
		// Liste des noms des ??crits d??j?? pr??sents dans la BDD.
		Vector<String> noms = new Vector<String>();
		// Liste des ??crits en trop ?? supprimer.
		Vector<Ecrit> toDel = new Vector<Ecrit>();
		for(Ecrit e : ecrits) {
			if(noms.contains(basicize(e.getNom()))) { // Si le nom existe d??j????: poubelle
				toDel.add(e);
			} else {
				noms.add(basicize(e.getNom())); // Sinon on enrigistre le nom.
			}
		}
		for(Ecrit e : toDel) { // Suppression des doublons.
			ecrits.remove(e);
			for(Affichan a : affichans) {
				a.remove(e);
			}
		}
	}
	
	/**
	 * Annule la derni??re action et revient ?? la derni??re sauvegarde.
	 */
	public boolean annuler() {
		if(cancel.empty()) { // Si aucune sauvegarde, pas de bol, tant pis
			return false;
		} else {
			ecrits = cancel.pop(); // Sinon on la r??cup??re en la sortant du tas et on remplace la BDD actuelle
			for(Affichan aff : affichans) { // Change les r??f??rences d'??crits dans tous les affichans
				aff.updateRefs(ecrits);
			}
			return true;
		}
	}
	
	public Vector<Ecrit> getEcrits() {
		return ecrits;
	}
	
	public BotCommand getCommand(String name) {
		return commands.get(name);
	}
	
	public JDA getJda() {
		return jda;
	}
	
	public Vector<SyndEntry> getInbox() {
		return inbox;
	}
	
	/**
	 * Archive la BDD actuelle dans la pile d'annulation.
	 */
	public void archiver() {
		// Effectue une copie profonde du vecteur, sinon les ??crits modifi??s dans la BDD le seront aussi dans les sauvegardes.
		cancel.add(new Vector<Ecrit>());
		for(Ecrit e : ecrits) {
			cancel.peek().add(e.clone());
		}
		if(cancel.size() > 20) { // Supprime les anciennes sauvegardes pour ??viter une surcharge de la m??moire.
			cancel.remove(0);
		}
	}
	
	/**
	 * Sheduler qui execute la recherche de flux RSS toutes les 10mn.
	 */
	private ScheduledExecutorService shreduler = Executors.newScheduledThreadPool(1);

	/*
	 * Map contenant les messages ?? plusieurs pages
	 */
	public LinkedHashMap<String, Vector<MessageEmbed>> multimessages = new LinkedHashMap<String, Vector<MessageEmbed>>();
	/*
	 * Map contenant les positions actuelles des messages ?? plusieurs pages
	 */
	public LinkedHashMap<String, Integer> mmposition = new LinkedHashMap<String, Integer>();
	
	@Override
	public void onEvent(GenericEvent event) {
		if(event instanceof ReadyEvent) { // Lorsque le bot est pr??t
			initCommands(); // Initialise les commandes
			// Intitialise le shreduler pour les v??rifications r??guli??res de nouveaux fils.
			shreduler.scheduleAtFixedRate(new Runnable() {

				@Override
				public void run() {
					try {
						int tailleAncienne = ecrits.size();
						addNew(); // V??rifie les nouveaux ??crits
						if(tailleAncienne != ecrits.size()) { // S'il y a des nouveaux ajouts, mettre ?? jour les messages (sinon c'est pas la peine).
							updateOpen();
						}
					} catch (IllegalArgumentException | FeedException | IOException e) {
						e.printStackTrace();
						jda.getTextChannelById(737725144390172714L).sendMessage("<@340877529973784586>\n" + e.getClass().getCanonicalName() + "\n" + e.getLocalizedMessage()).queue();
					}
					System.out.println("Shreduled update.");
				}
				
			}, 10, 10, TimeUnit.MINUTES);
			jda.getPresence().setActivity(Activity.playing("critiquer. Aucune mise ?? jour forum depuis le red??marrage."));
			for(Affichan aff : affichans) {
				try {
					aff.initialize(this);
				} catch (InterruptedException | ExecutionException e) {
					e.printStackTrace();
				}
			}

		}
		if(event instanceof MessageDeleteEvent) {
			for(Affichan aff : affichans) {
				aff.checkDeletion(this, (MessageDeleteEvent) event);
			}
		} else if(event instanceof ButtonInteractionEvent) {
			ButtonInteractionEvent bie = (ButtonInteractionEvent) event;
			if(bie.getButton().getId().startsWith("e")) { // Boutons sur les messages d?????crits
				int id = Integer.parseInt(bie.getButton().getId().split("-")[1]);
				Ecrit e = Affichan.searchByHash(id, ecrits);
				if(e == null) {
					System.err.println("Attention???: bouton ne correspondant ?? aucun ??crit.");
					bie.editComponents(bie.getMessage().getActionRows().get(0).asDisabled()).queue();
				} else {
					if(bie.getButton().getId().endsWith("m")) {
						bie.reply("Choisissez le type de votre marque.").addActionRow(InteretType.actionRow(e)).setEphemeral(true).queue();
						return;
					} else if(bie.getButton().getId().endsWith("c")) {
						archiver();
						jda.getTextChannelById(organichan).sendMessage("????" + e.getNom() + "???? critiqu????!").queue();
						e.critique();
					} else if(bie.getButton().getId().endsWith("r")) {
						archiver();
						e.setStatus(Status.REFUSE);
						jda.getTextChannelById(organichan).sendMessage("????" + e.getNom() + "???? refus????!").queue();
					} else if(bie.getButton().getId().endsWith("d")) {
						archiver();
						e.liberer(bie.getMember());
					} else if(bie.getButton().getId().endsWith("u")) {
						archiver();
						e.setStatus(Status.OUVERT);
					} else if(bie.getButton().getId().endsWith("p") ) {
						archiver();
						e.setStatus(Status.PUBLIE);
					}
					bie.editMessageEmbeds(e.toEmbed()).complete();
					updateOpen();
					try { // Essaye de sauvegarder
						save();
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				}
				
			} else if(bie.getButton().getId().startsWith("mm")) { // Boutons des messages ?? plusieurs pages
				try {
					String id = bie.getButton().getId().split("-")[0];
					int next = bie.getButton().getId().split("-")[1].equals("n") ? 1 : -1;
					Vector<MessageEmbed> multimessage = multimessages.get(id); // On r??cup??re la liste des pages avec l???identifiant du bouton
					int position = mmposition.get(id) + next; // Et la prochaine position
					mmposition.put(id, position);
					// On ??dite le message
					bie.editMessageEmbeds(multimessage.get(position)).queue();
					// Et on d??sactive certains boutons si n??cessaire (bout de la liste)
					Button bPrec = bie.getMessage().getActionRows().get(0).getButtons().get(0);
					Button bNext = bie.getMessage().getActionRows().get(0).getButtons().get(1);
					if(position == 0) {
						bie.getHook().editOriginalComponents(ActionRow.of(bPrec.asDisabled(), bNext.asEnabled())).queue();
					} else if(position == multimessage.size() - 1) {
						bie.getHook().editOriginalComponents(ActionRow.of(bPrec.asEnabled(), bNext.asDisabled())).queue();
					} else {
						bie.getHook().editOriginalComponents(ActionRow.of(bPrec.asEnabled(), bNext.asEnabled())).queue();
					}
				} catch(NullPointerException e) { // Si le message n???a pas ??t?? trouv?? (bot reboot), on d??sactive les boutons
					bie.editComponents(bie.getMessage().getActionRows().get(0).asDisabled()).queue();
				}
			} else if(bie.getButton().getId().startsWith("tm")) { // Boutons de choix de marque d???int??r??t
				int id = Integer.parseInt(bie.getButton().getId().split("-")[1]);
				Ecrit e = Affichan.searchByHash(id, ecrits);
				if(e == null) {
					System.err.println("Attention???: bouton ne correspondant ?? aucun ??crit.");
					bie.editComponents(bie.getMessage().getActionRows().get(0).asDisabled()).queue();
				} else {
					archiver();
					String type = bie.getButton().getId().split("-")[2];
					if(e.hasMarque(bie.getMember())) {
						e.liberer(bie.getMember());
					}
					InteretType it = InteretType.getInteretType(type);
					e.marquer(bie.getMember(), it);
					bie.editMessage("??crit r??serv??.").setActionRows().queue();
				}
				updateOpen();
				try { // Essaye de sauvegarder
					save();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		} else if(event instanceof MessageReceivedEvent) { // Message re??u
			MessageReceivedEvent mre = (MessageReceivedEvent) event;
			
			// Cas particulier o?? c???est un message dans le salon organisation et qu???il contient un lien forum
			// R??serve dans ce cas l?????crit indiqu??, le mettant en ouvert s???il ne l???est pas (outrepasse les protections)
			if(mre.getMessage().getContentRaw().contains("fondationscp.wikidot.com/forum") && mre.getChannel().getIdLong() == organichan) {
				// R??cup??re l???ID forum du fil
				int idForum = Integer.parseInt(mre.getMessage().getContentRaw().split("/t-", 2)[1].split("/")[0]);
				Ecrit e = Affichan.searchByHash(idForum, ecrits);
				if(e != null) {
					archiver();
					if(e.getStatus() != Status.OUVERT_PLUS) // ??vite de supprimer les marques d??j?? pr??sentes s???il y en a
						e.setStatus(Status.OUVERT);
					e.marquer(mre.getMember(), InteretType.INSTANT);
					updateOpen();
				} // Sinon tant pis
				
			} // Comme il n???y a pas de commande dans le message, la fonction retournera ?? la prochaine condition
			
			// Termine directement si ce n'est pas une commande ou un message envoy?? par un bot.
			if(!mre.getMessage().getContentRaw().startsWith(prefix + "!") || mre.getAuthor().isBot() || mre.getAuthor().getId().equals(jda.getSelfUser().getId()))
				return;
			
			// R??cup??re la commande
			String command = mre.getMessage().getContentRaw().split(" ", 2)[0].split("!", 2)[1];
			
			// R??cup??re les arguments
			String args;
			try {
				args = mre.getMessage().getContentRaw().split(" ", 2)[1];
			} catch(ArrayIndexOutOfBoundsException e) {
				args = "";
			}
			// Log la commande
			System.out.println(command);
			System.out.println(args);
			System.out.println("??????????????????????????????");
			
			MessageChannel chan = mre.getChannel();
			
			// Essaye d'executer la commande demand??e
			try {
				String[] str = args.split(";");
				if(str.length == 1 && str[0] == "") {
					str = new String[0];
				}
				commands.get(command).execute(this, mre, str);
			} catch(NullPointerException e) { // Si elle n'est pas trouv??e, c'est qu'elle est inconnue.
				chan.sendMessage("Commande inconnue.").queue();
			}
			
			//Met ?? jour les messages et sauvegarde apr??s chaque commande.
			updateOpen();
			try {
				save();
			} catch (IOException e) {
				if(!errorSave) {
					mre.getChannel().sendMessage("Impossible de sauvegarder les donn??es. Je vais continuer ?? essayer, mais n'enverrai pas d'autres messages si cela ??choue.").queue();
					e.printStackTrace();
					errorSave = true;
				}
			}
		} else if(event instanceof SlashCommandInteractionEvent) {
			SlashCommandInteractionEvent scie = (SlashCommandInteractionEvent) event;
			try {
				BotCommand command = commands.get(scie.getName());
				command.slash(this, scie);
			} catch(NullPointerException e) {
				System.err.println("Commande slash " + scie.getCommandString() + " non trouv??e.");
				if(scie.isAcknowledged()) {
					scie.getHook().sendMessage("Navr??, il y a eu une erreur.").queue();
				} else {
					scie.reply("Navr??, il y a eu une erreur.").queue();
				}
				e.printStackTrace();
			}
			//Met ?? jour les messages et sauvegarde apr??s chaque commande.
			updateOpen();
			try {
				save();
			} catch (IOException e) {
				if(!errorSave) {
					scie.getHook().sendMessage("Impossible de sauvegarder les donn??es. Je vais continuer ?? essayer, mais n'enverrai pas d'autres messages si cela ??choue.").queue();
					e.printStackTrace();
					errorSave = true;
				}
			}
		}
		
	}

}
