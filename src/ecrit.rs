use std::collections::HashMap;
use std::str::FromStr;

use chrono::DateTime;
use fondabots_lib;
use fondabots_lib::object::Field;
use fondabots_lib::tools::basicize;
use fondabots_lib::DataType;
use fondabots_lib::ErrType;
use fondabots_lib::{try_loop, Bot, Object};
use poise::serenity_prelude as serenity;
use regex::Regex;
use rss::Channel;
use serenity::all::Context as SerenityContext;
use serenity::all::{ButtonStyle, ComponentInteraction, CreateActionRow, CreateButton, CreateEmbed, CreateEmbedFooter, CreateInteractionResponse, CreateInteractionResponseMessage, CreateMessage, EditMessage, Timestamp};
use serenity::builder::CreateEmbedAuthor;
use yaml_rust2::yaml;
use yaml_rust2::Yaml;

use fields::Interet;
use fields::Status;
use fields::Type;

pub mod fields;

#[derive(Clone, PartialEq, Debug)]
pub struct Ecrit {
    pub status: Status,
    pub type_: Type,
    pub nom: String,
    pub lien: String,
    pub last_update: Timestamp,
    pub auteur: String,
    interesses: Vec<Interet>,
    pub modified: bool,
    pub tags: Vec<String>,
    id: u64,
}

impl Ecrit {
    pub fn new(
        nom: String,
        lien: String,
        type_: Type,
        status: Status,
        auteur: String,
    ) -> Result<Self, ErrType> {
        Ok(Self {
            status,
            type_,
            nom,
            id: Self::find_id(&lien).unwrap(),
            lien,
            last_update: Timestamp::now(),
            auteur,
            interesses: Vec::new(),
            modified: false,
            tags: Vec::new(),
        })
    }

    pub fn critique(&mut self) {
        self.last_update = Timestamp::now();
        self.delete_interet();
        self.status = Status::EnAttente;
        self.modified = true;
    }

    pub fn delete_interet(&mut self) {
        self.interesses.clear();
    }

    pub fn liberer_id(&mut self, membre: u64) -> bool {
        if membre == 0 {
            return false;
        }
        let mut index: usize = 0;
        for interet in &self.interesses {
            if interet.member == membre {
                break;
            }
            index += 1;
        }
        if index == self.interesses.len() {
            false
        } else {
            self.interesses.remove(index);
            if self.interesses.len() == 0 {
                self.status = Status::Ouvert;
            }
            true
        }
    }

    pub fn liberer_name(&mut self, membre: &String) -> bool {
        let mut index: usize = 0;
        for interet in &self.interesses {
            if interet.name == *membre {
                break;
            }
            index += 1;
        }
        if index == self.interesses.len() {
            false
        } else {
            self.interesses.remove(index);
            if self.interesses.len() == 0 {
                self.status = Status::Ouvert;
            }
            true
        }
    }

    pub fn find_id(url: &String) -> Option<u64> {
        let regex_id = Regex::new(r"t-(\d+)/?").unwrap();
        if let Some(v) = regex_id.captures(url.as_str()) {
            if let Some(w) = v.extract::<1>().1.get(0) {
                if let Ok(ret) = w.parse() {
                    return Some(ret);
                }
            }
        }
        None
    }

    pub fn marquer(&mut self, interet: Interet) {
        self.liberer_id(interet.member);
        self.liberer_name(&interet.name);
        self.interesses.push(interet);
        self.status = Status::OuvertPlus;
        self.modified = true;
    }

    pub fn recherche_auteur<'a>(critere: &String, database: &'a HashMap<u64, Self>) -> Vec<&'a String> {
        let mut ret = Vec::new();
        for (_, ecrit) in database {
            let mut ok = true;
            for mot_s in critere.split(" ") {
                let mut found = false;
                for mot in ecrit.auteur.split(" ") {
                    found = found || basicize(mot).contains(&basicize(mot_s));
                }
                if !found {
                    ok = false;
                    break;
                }
            }
            if ok {
                ret.push(&ecrit.auteur);
            }
        }
        ret
    }

    pub fn ulister<'a>(
        bot: &'a Bot<Self>,
        critere: String,
        status: Vec<Status>,
        types: Vec<Type>,
        authors: Vec<&'a String>,
        tags: Vec<String>,
        tags_et: bool,
        modifie_avant: Option<Timestamp>,
        modifie_apres: Option<Timestamp>
    ) -> Vec<&'a u64> {
        if critere.is_empty() {
            bot.database.keys().collect()
        } else {
            bot.search(&critere)
        }.into_iter()
            .map(|ecrit_id| bot.database.get(ecrit_id).unwrap())
            .filter(|ecrit| status.contains(&ecrit.status) || status.is_empty())
            .filter(|ecrit| types.contains(&ecrit.type_) || types.is_empty())
            .filter(|ecrit| authors.contains(&&ecrit.auteur) || authors.is_empty())
            .filter(|ecrit| {
                let tag_list: Vec<bool> = ecrit.tags.iter().map(|tag| {tags.contains(tag)}).collect();
                tag_list.is_empty() ||
                (!tags_et && tag_list.contains(&true)) ||
                (tags_et && tag_list.into_iter().fold(true, |accumulator, has_tag| {accumulator && has_tag}))
            })
            .filter(|ecrit| modifie_avant.is_none() || ecrit.last_update < modifie_avant.unwrap())
            .filter(|ecrit| modifie_apres.is_none() || ecrit.last_update > modifie_apres.unwrap())
            .map(|ecrit| &ecrit.id)
            .collect()
    }
}


impl Object for Ecrit {
    fn new() -> Self {
        Self {
            status: Status::Inconnu,
            type_: Type::Autre,
            nom: String::new(),
            lien: String::new(),
            last_update: Timestamp::now(),
            auteur: String::new(),
            interesses: Vec::new(),
            modified: false,
            tags: Vec::new(),
            id: 0,
        }
    }

    fn get_id(&self) -> u64 {
        self.id.clone()
    }

    fn from_yaml(data: &Yaml) -> Result<Self, ErrType> {
        let data_hash = data;
        /*.ok_or(ErrType::YamlParseError(format!("Entrée d’écrit invalide (n’est pas un hash).")))?;*/
        let lien = data_hash["lien"].as_str().ok_or(ErrType::YamlParseError("Erreur de yaml dans un champ lien.".to_string()))?.to_string();
        Ok(Self {
            nom: data_hash["nom"].as_str().ok_or(ErrType::YamlParseError("Erreur de yaml dans un champ nom.".to_string()))?.to_string(),
            status: Status::from_str(data_hash["status"].as_str().ok_or(ErrType::YamlParseError("Erreur de yaml dans un status.".to_string()))?)?,
            type_: Type::from_str(data_hash["type"].as_str().ok_or(ErrType::YamlParseError("Erreur de yaml dans un type.".to_string()))?)?,
            auteur: data_hash["auteur"].as_str().ok_or(ErrType::YamlParseError("Erreur de yaml dans un auteur.".to_string()))?.to_string(),
            interesses: data_hash["interesses"].as_vec().ok_or(ErrType::YamlParseError("Erreur de yaml dans un interesses.".to_string()))?.iter().map(
                |interet| -> Interet {
                    Interet {
                        name: interet["name"].as_str().unwrap().to_string(),
                        date: Timestamp::from_unix_timestamp(interet["date"].as_i64().unwrap().try_into().unwrap()).unwrap(),
                        type_: interet["type"].as_str().unwrap().to_string(),
                        member: interet["member"].as_i64().unwrap().try_into().unwrap(),
                    }
                }
            ).collect(),
            modified: data_hash["edited"].as_bool().ok_or(ErrType::YamlParseError("Erreur de yaml dans un edited.".to_string()))?,
            tags: data_hash["tags"].as_vec().ok_or(ErrType::YamlParseError("Erreur de yaml dans un status.".to_string()))?.iter().map(
                |tag| -> String {
                    tag.as_str().unwrap().to_string()
                }
            ).collect(),
            last_update: Timestamp::from_unix_timestamp(data["lastUpdate"].as_i64()
                .ok_or(ErrType::YamlParseError("Erreur de yaml dans un last_update.".to_string()))?.try_into()?)?,
            id: Ecrit::find_id(&lien).ok_or(ErrType::NoneError)?,
            lien,
        })
    }

    fn serialize(&self) -> Yaml {
        let mut yaml_out = yaml::Hash::new();
        yaml_out.insert(Yaml::String("nom".to_string()), Yaml::String(self.nom.clone()));
        yaml_out.insert(Yaml::String("lien".to_string()), Yaml::String(self.lien.clone()));
        yaml_out.insert(Yaml::String("type".to_string()), Yaml::String(self.type_.to_string()));
        yaml_out.insert(Yaml::String("status".to_string()), Yaml::String(self.status.to_string()));
        yaml_out.insert(Yaml::String("lastUpdate".to_string()), Yaml::Integer(self.last_update.timestamp()));
        yaml_out.insert(Yaml::String("auteur".to_string()), Yaml::String(self.auteur.clone()));
        yaml_out.insert(Yaml::String("edited".to_string()), Yaml::Boolean(self.modified.clone()));
        let mut array_interet = yaml::Array::new();
        for interet in &self.interesses {
            let mut hash_interet = yaml::Hash::new();
            hash_interet.insert(Yaml::String("name".to_string()), Yaml::String(interet.name.to_string()));
            hash_interet.insert(Yaml::String("date".to_string()), Yaml::Integer(interet.date.timestamp()));
            hash_interet.insert(Yaml::String("type".to_string()), Yaml::String(interet.type_.to_string()));
            hash_interet.insert(Yaml::String("member".to_string()), Yaml::Integer(interet.member as i64));
            array_interet.push(Yaml::Hash(hash_interet));
        }
        yaml_out.insert(Yaml::String("interesses".to_string()), Yaml::Array(array_interet));
        yaml_out.insert(Yaml::String("tags".to_string()), Yaml::Array(
            self.tags.iter().map(
                |tag| -> Yaml {
                    Yaml::String(tag.clone())
                }
            ).collect()
        ));
        Yaml::Hash(yaml_out)
    }

    fn is_modified(&self) -> bool {
        self.modified
    }

    fn set_modified(&mut self, modified: bool) {
        self.modified = modified;
    }

    fn get_embed(&self) -> CreateEmbed {
        let mut fields = vec![
            (Type::field_name(), self.type_.to_string(), false),
            (Status::field_name(), self.status.to_string(), false),
        ];
        if self.status == Status::OuvertPlus {
            let mut interets_list = String::new();
            for interet in &self.interesses {
                interets_list += format!("{} par {} le {}\n", interet.type_, interet.name, interet.date.format("%d %B %Y à %H:%M")).as_str();
            }
            fields.push(("Marques d’intérêt", interets_list, false));
        }
        if !self.tags.is_empty() {
            let mut total = String::new();
            for tag in &self.tags {
                total += format!("{tag}\n").as_str();
            }
            fields.push(("Tags", total, false));
        }
        CreateEmbed::new()
            .title(self.nom.clone())
            .url(self.lien.clone())
            .fields(fields)
            .footer(CreateEmbedFooter::new(self.id.to_string()))
            .author(CreateEmbedAuthor::new(&self.auteur))
            .timestamp(&self.last_update)
            .color(self.type_.get_color())
    }

    fn get_buttons(&self) -> CreateActionRow {
        let id = &self.id;
        let marque = CreateButton::new(format!("e-{id}-m")).style(ButtonStyle::Primary).label("Marquer");
        let critique = CreateButton::new(format!("e-{id}-c")).style(ButtonStyle::Success).label("Critiqué");
        let refus = CreateButton::new(format!("e-{id}-r")).style(ButtonStyle::Danger).label("Refusé");
        let retirer = CreateButton::new(format!("e-{id}-d")).style(ButtonStyle::Secondary).label("Retirer marque");
        let up = CreateButton::new(format!("e-{id}-u")).style(ButtonStyle::Success).label("Up");
        let publie = CreateButton::new(format!("e-{id}-p")).style(ButtonStyle::Success).label("Publié");
        let no = CreateButton::new(format!("e-{id}-0")).style(ButtonStyle::Primary).label("Aucune action possible").disabled(true);
        let mut buttons = Vec::new();

        if self.status == Status::Ouvert || self.status == Status::OuvertPlus {
            buttons.push(marque);
            buttons.push(critique);
        }
        if vec![Status::Infraction, Status::SansNouvelles, Status::EnAttente, Status::EnPause, Status::Inconnu].contains(&self.status) {
            buttons.push(up);
        }
        if (self.type_ == Type::Rapport || self.type_ == Type::Idee) && self.status != Status::Refuse && self.status != Status::Publie && self.status != Status::Valide {
            buttons.push(refus);
        }
        if self.status == Status::Ouvert || self.status == Status::OuvertPlus {
            buttons.push(retirer);
        }
        if self.status == Status::Valide {
            buttons.push(publie);
        }
        if buttons.is_empty() {
            buttons.push(no);
        }
        CreateActionRow::Buttons(buttons)
    }

    fn get_name(&self) -> &String {
        &self.nom
    }

    fn set_name(&mut self, s: String) {
        self.nom = s;
    }

    fn get_list_entry(&self) -> String {
        format!("[**{}**]({})\n{}\n{}\n{}\n\n", self.nom, self.lien, self.auteur, self.status, self.type_)
    }

    fn up(&mut self) {
        if self.status != Status::OuvertPlus {
            self.status = Status::Ouvert;
        }
    }

    async fn buttons(ctx: &SerenityContext, interaction: &mut ComponentInteraction, bot: &mut Bot<Self>) -> Result<(), ErrType> {
        let parts: Vec<&str> = interaction.data.custom_id.split("-").collect();
        let button_type = *parts.get(0)
            .ok_or(ErrType::InteractionIDError(interaction.data.custom_id.clone(), interaction.message.id.get()))?;
        match button_type {
            "e" => {
                let id: u64 = parts.get(1)
                    .ok_or(ErrType::InteractionIDError(interaction.data.custom_id.clone(), interaction.message.id.get()))?.parse()?;
                let action = *parts.get(2).ok_or(ErrType::InteractionIDError(interaction.data.custom_id.clone(), interaction.message.id.get()))?;
                match action {
                    "m" => {
                        interaction.create_response(ctx, CreateInteractionResponse::Message(
                            CreateInteractionResponseMessage::new().content("Choisissez le type de votre marque.")
                                .components(vec![Interet::action_row(id)]).ephemeral(true))).await?;
                    }
                    "c" => {
                        interaction.create_response(ctx, CreateInteractionResponse::Acknowledge).await?;
                        bot.get_absolute_chan("organichan")?
                            .send_message(ctx, CreateMessage::new().content(format!("« {} » critiqué !",
                                                                                    bot.database.get(&id).ok_or(ErrType::ObjectNotFound(id.to_string()))?.get_name()))).await?;
                        bot.archive(vec![id]);
                        bot.database.get_mut(&id).unwrap()/* Error check already done above */.critique();
                    }
                    "r" => {
                        interaction.create_response(ctx, CreateInteractionResponse::Acknowledge).await?;
                        bot.get_absolute_chan("organichan")?
                            .send_message(ctx, CreateMessage::new().content(format!("« {} » refusé !",
                                                                                    bot.database.get(&id).ok_or(ErrType::ObjectNotFound(id.to_string()))?.get_name()))).await?;
                        bot.archive(vec![id]);
                        bot.database.get_mut(&id).unwrap()/* Error check already done above */.status = Status::Refuse;
                    }
                    "d" => {
                        interaction.create_response(ctx, CreateInteractionResponse::Acknowledge).await?;
                        if bot.database.contains_key(&id) {
                            bot.archive(vec![id]);
                            bot.database.get_mut(&id).unwrap()
                                .liberer_name(interaction.member.as_ref().unwrap().nick.as_ref().unwrap_or(&interaction.member.as_ref().unwrap().user.name));
                        } else {
                            return Err(ErrType::ObjectNotFound(id.to_string()));
                        }
                    }
                    "u" => {
                        interaction.create_response(ctx, CreateInteractionResponse::Acknowledge).await?;
                        if bot.database.contains_key(&id) {
                            bot.archive(vec![id]);
                            bot.database.get_mut(&id).unwrap().status = Status::Ouvert;
                            bot.database.get_mut(&id).unwrap().modified = true;
                        } else {
                            return Err(ErrType::ObjectNotFound(id.to_string()));
                        }
                    }
                    "p" => {
                        interaction.create_response(ctx, CreateInteractionResponse::Acknowledge).await?;
                        if bot.database.contains_key(&id) {
                            bot.archive(vec![id]);
                            bot.database.get_mut(&id).unwrap().status = Status::Publie;
                            bot.database.get_mut(&id).unwrap().modified = true;
                        } else {
                            return Err(ErrType::ObjectNotFound(id.to_string()));
                        }
                    }
                    _ => {
                        interaction.create_response(ctx, CreateInteractionResponse::Acknowledge).await?;
                        eprintln!("Action inconnue pressée sur un bouton: e-{id}-{action}");
                    }
                }
                let ecrit = bot.database.get(&id);
                interaction.message.edit(ctx, EditMessage::new().embed(
                    ecrit.ok_or(ErrType::ObjectNotFound(id.to_string()))?.get_embed()
                ).components(vec![ecrit.unwrap().get_buttons()])).await?;
                bot.update_affichans(ctx).await?;
                bot.save()?;
            }
            "tm" => {
                let id: u64 = parts.get(1)
                    .ok_or(ErrType::InteractionIDError(interaction.data.custom_id.clone(), interaction.message.id.get()))?.parse()?;
                let type_ = *parts.get(2).ok_or(ErrType::InteractionIDError(interaction.data.custom_id.clone(), interaction.message.id.get()))?;
                if bot.database.contains_key(&id) {
                    bot.archive(vec![id]);
                    let ecrit = bot.database.get_mut(&id).unwrap();
                    let member = interaction.member.as_ref().unwrap();
                    ecrit.marquer(Interet {
                        name: interaction.member.as_ref().unwrap().nick.as_ref().unwrap_or(&interaction.member.as_ref().unwrap().user.name).clone(),
                        date: Timestamp::now(),
                        type_: Interet::get_type(type_).to_string(),
                        member: member.user.id.get(),
                    });
                    interaction.create_response(ctx, CreateInteractionResponse::UpdateMessage(
                        CreateInteractionResponseMessage::new().content("Écrit marqué.").components(vec![]).ephemeral(true))).await?;
                } else {
                    interaction.create_response(ctx, CreateInteractionResponse::Acknowledge).await?;
                    return Err(ErrType::ObjectNotFound(id.to_string()));
                }
            }
            _ => { interaction.create_response(ctx, CreateInteractionResponse::Acknowledge).await?; }
        }

        Ok(())
    }


    async fn maj_rss(bot: &DataType<Self>) -> Result<(), ErrType> {
        let url = "http://fondationscp.wikidot.com/feed/forum/ct-656675.xml";
        let regex_balises = Regex::new(r##"\s*\[([^\[]*)]"##).unwrap();
        /* OH FUCK */
        let regex_titres = Regex::new(r##"(?i)\s*(?:\s*[\[(][^\[]*?[])][\s/\\\-]*)*(?:scp(?:[-\s][\dXY#█?]+(?:[-\s]fr)?)?)?[\s:\-"]*([^"]*?(?:"[^"]+"?[^"]*?)*)[\s".]*(?:\(.*(?:provisoire|temporaire|version).*\))?[\s".]*$"##).unwrap();
        let bot = &mut bot.lock().await;
        let mut last_date = DateTime::from_timestamp(0, 0).unwrap();
        let rss = Channel::read_from(&reqwest::get(url).await?.bytes().await?[..])?;
        for entry in &rss.items {
            let date = try_loop!(DateTime::parse_from_rfc2822(entry.pub_date.as_ref().unwrap().as_str()), "Erreur lors de la récupération des flux RSS: pas de date.").to_utc();
            if date > bot.last_rss_update {
                if entry.title.as_ref().is_some_and(|str| { str.contains("]") }) {
                    let mut type_ = Type::Rapport;
                    for balise in regex_balises.captures_iter(entry.title.as_ref().unwrap()) {
                        let balise = balise.extract::<1>().0.trim().to_lowercase();
                        if balise.contains("idée") || balise.contains("idee") {
                            type_ = Type::Idee;
                        } else if balise.contains("conte") || balise.contains("série") || balise.contains("serie") {
                            type_ = Type::Conte;
                        } else if balise.contains("format") {
                            type_ = Type::FormatGdi;
                        }
                    }
                    let mut title = try_loop!(try_loop!(regex_titres.captures(entry.title.as_ref().unwrap())
                                .ok_or(ErrType::NoneError), "Erreur lors de l’interprétation du titre.").extract::<1>().1.get(0)
                                .ok_or(ErrType::NoneError), "Erreur lors de l’interprétation du titre.").to_string();
                    if title.is_empty() {
                        title = format!("(sans nom {})", bot.search("sans nom").len());
                    }

                    let lien = try_loop!(entry.link.clone().ok_or(ErrType::NoneError), "Pas de lien dans une entrée RSS.");
                    let id: u64 = try_loop!(Ecrit::find_id(&lien).ok_or(ErrType::NoneError), "Lien mal formé dans une entrée RSS.");
                    let auteur = if let Some(extensions) = entry.extensions().get("wikidot") {
                        if let Some(author_names) = extensions.get("authorName") {
                            if let Some(author_name) = author_names.get(0) {
                                author_name.value()
                            } else {None}
                        } else {None}
                    } else {None};

                    let ecrit = Ecrit {
                        status: Status::Ouvert,
                        type_,
                        nom: title,
                        lien,
                        last_update: Timestamp::now(),
                        auteur: try_loop!(auteur.ok_or(ErrType::NoneError), "Pas d’auteur dans une entrée RSS.").to_string(),
                        interesses: vec![],
                        modified: false,
                        tags: vec![],
                        id,
                    };
                    if !bot.database.contains_key(&id) {
                        bot.database.insert(id, ecrit);
                    } else {
                        eprintln!("Ajout RSS d’un écrit déjà ajouté. Informations : écrit [{}] - last_rss_update [{}] - last_date [{}] - date>last_rss_update [{}]", date, bot.last_rss_update, last_date, date > bot.last_rss_update);
                    }

                }
            }
            if date > last_date {
                last_date = date;
            }
        }
        bot.last_rss_update = last_date;
        bot.update_affichans = true;
        Ok(())
    }

    fn get_date(&self) -> &Timestamp {
        &self.last_update
    }

    fn set_date(&mut self, t: Timestamp) {
        self.last_update = t;
    }
}