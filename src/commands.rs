use std::collections::HashMap;
use std::str::FromStr;

use fondabots_lib::command_data::{CommandData, Permission};
use fondabots_lib::{
    generic_commands,
    tools,
    tools::{alias, basicize, get_object, parse_date},
    ErrType,
    Object
};
use poise::{Command, Context, CreateReply};
use rand::prelude::*;
use serenity::all::{CreateEmbed, CreateEmbedAuthor, CreateEmbedFooter, Timestamp};

use crate::{
    ecrit::fields::Type,
    ecrit::fields::{Interet, Status},
    ecrit::Ecrit,
    DataType
};

/// Ajoute manuellement un écrit à la base de données.
#[poise::command(slash_command, category = "Édition", custom_data = CommandData::perms(Permission::WRITE), check = CommandData::check)]
pub async fn ajouter(
    ctx: Context<'_, DataType, ErrType>,
    #[description = "Nom de l’écrit"] nom: String,
    #[description = "Auteur de l’écrit"] auteur: String,
    #[description = "Type de l’écrit"] type_: Type,
    #[description = "Status de l’écrit"] status: Status,
    #[description = "Lien forum de l’écrit"] url: String
) -> Result<(), ErrType> {
    let bot = &mut ctx.data().lock().await;
    if let Some(id) = Ecrit::find_id(&url) {
        bot.database.insert(id, Ecrit::new(nom.clone(), url, type_, status, auteur)?);
        ctx.say(format!("Écrit « {nom} » ajouté !")).await?;
        bot.log(&ctx, format!("{} a ajouté l'écrit {nom} (id: {id})", tools::user_desc(ctx.author()))).await?;
    } else {
        ctx.say("URL malformée, impossible de déterminer l’identifiant de l’écrit.").await?;
    }
    Ok(())
}

/// Liste tous les écrits d’un certain type ou status.
#[poise::command(slash_command, category = "Recherche", custom_data = CommandData::perms(Permission::READ), check = CommandData::check)]
pub async fn lister(
    ctx: Context<'_, DataType, ErrType>,
    #[description = "Status recherché"] status: Option<Status>,
    #[description = "Type recherché"] type_: Option<Type>
) -> Result<(), ErrType> {
    generic_commands::lister_two(ctx, status, type_).await
}

/// Nettoie la base de données en supprimant les écrits abandonnés, publiés et refusés.
#[poise::command(slash_command, category = "Base de données", custom_data = CommandData::perms(Permission::MANAGE), check = CommandData::check)]
pub async fn nettoyer(ctx: Context<'_, DataType, ErrType>) -> Result<(), ErrType> {
    let bot = &mut ctx.data().lock().await;
    let list: Vec<u64> = bot.database.iter().filter(
        | (_, ecrit) | {
            ecrit.status == Status::Abandonne || ecrit.status == Status::Publie || ecrit.status == Status::Refuse
        }
    ).map(
        | (&id, _) | {id}
    ).collect();
    let nb_deleted = list.len();
    bot.archive(list.clone());
    list.into_iter().for_each(
        | id | {
            bot.database.remove(&id);
        }
    );
    ctx.say(format!("{nb_deleted} écrit(s) abandonné(s), publié(s) et refusé(s) supprimé(s) de la liste.")).await?;
    bot.log(&ctx, format!("{} a nettoyé la base de données. {nb_deleted} écrits supprimés.", tools::user_desc(ctx.author()))).await?;
    Ok(())
}

/// Change le statut d’un écrit.
#[poise::command(slash_command, category = "Édition", custom_data = CommandData::perms(Permission::WRITE), check = CommandData::check)]
pub async fn statut(ctx: Context<'_, DataType, ErrType>,
                    #[description = "Critère d’identification de l’écrit"] critere: String,
                    #[description = "Nouveau statut"] statut: Status) -> Result<(), ErrType> {
    generic_commands::change_field(ctx, critere, statut).await
}

/// Change le type d’un écrit.
#[poise::command(slash_command, category = "Édition", rename = "type", custom_data = CommandData::perms(Permission::WRITE), check = CommandData::check)]
pub async fn type_(ctx: Context<'_, DataType, ErrType>,
                   #[description = "Critère d’identification de l’écrit"] critere: String,
                   #[description = "Nouveau type"]
                   #[rename = "type"]
                   type_: Type) -> Result<(), ErrType> {
    generic_commands::change_field(ctx, critere, type_).await
}

/// Valide un écrit. Si c’est une idée, change son type en rapport.
#[poise::command(slash_command, category = "Édition", custom_data = CommandData::perms(Permission::MANAGE), check = CommandData::check)]
pub async fn valider(ctx: Context<'_, DataType, ErrType>,
                     #[description = "Critère d’identification de l’écrit"] critere: String) -> Result<(), ErrType> {
    let bot = &mut ctx.data().lock().await;
    if let Some(object_id) = get_object(&ctx, bot, &critere).await? {
        bot.archive(vec![object_id]);
        let ecrit = bot.database.get_mut(&object_id).unwrap();
        if ecrit.type_ == Type::Idee {
            ecrit.type_ = Type::Rapport;
            ecrit.status = Status::EnAttente;
            ctx.say(format!("Idée « {} » validée !", ecrit.get_name())).await?;
        } else {
            ecrit.status = Status::Valide;
            ctx.say(format!("Écrit « {} » validé !", ecrit.get_name())).await?;
        }
        ecrit.modified = true;
        let ecrit = bot.database.get(&object_id).unwrap();
        bot.log(&ctx, format!("{} a validé l'écrit {} (id: {object_id})", tools::user_desc(ctx.author()), ecrit.get_name())).await?;

    }
    Ok(())
}

/// Refuse un écrit.
#[poise::command(slash_command, category = "Édition", custom_data = CommandData::perms(Permission::MANAGE), check = CommandData::check)]
pub async fn refuser(ctx: Context<'_, DataType, ErrType>,
                     #[description = "Critère d’identification de l’écrit"] critere: String) -> Result<(), ErrType> {
    let bot = &mut ctx.data().lock().await;
    if let Some(object_id) = get_object(&ctx, bot, &critere).await? {
        bot.archive(vec![object_id]);
        let ecrit = bot.database.get_mut(&object_id).unwrap();
        ecrit.status = Status::Refuse;
        ecrit.modified = true;
        ctx.say(format!("Écrit « {} » refusé !", ecrit.get_name())).await?;
        let ecrit = bot.database.get(&object_id).unwrap();
        bot.log(&ctx, format!("{} a refusé l'écrit {} (id: {object_id})", tools::user_desc(ctx.author()), ecrit.get_name())).await?;
    }
    Ok(())
}

/// Marque d’intérêt un écrit.
#[poise::command(slash_command, category = "Édition", custom_data = CommandData::perms(Permission::WRITE), check = CommandData::check)]
pub async fn marquer(ctx: Context<'_, DataType, ErrType>,
                     #[description = "Critère d’identification de l’écrit"] critere: String,
                     #[description = "Type de l’intérêt"]
                     #[choices("⊙ Exclusif", "⊟ Immédiat", "⋄ Ouvert", "∙ Intérêt simple", "⋇ Collab recherchée")]
                     type_: &str,
                     #[description = "Nom de la personne qui marque l’écrit si ce n’est pas la personne exécutant la commande"] procuration: Option<String>) -> Result<(), ErrType> {
    let bot = &mut ctx.data().lock().await;
    if let Some(object_id) = get_object(&ctx, bot, &critere).await? {
        if bot.database.get(&object_id).unwrap().status != Status::OuvertPlus || bot.database.get(&object_id).unwrap().status != Status::Ouvert {
            bot.archive(vec![object_id]);
            let ecrit = bot.database.get_mut(&object_id).unwrap();
            let author_member = ctx.author_member().await.ok_or(ErrType::Generic)?;
            let member_name = procuration.as_ref().unwrap_or(
                author_member.nick.as_ref().unwrap_or(&author_member.user.name)
            );
            ctx.say(format!("Écrit « {} » marqué d’intérêt pour {member_name}", ecrit.get_name())).await?;

            ecrit.marquer(Interet {
                name: member_name.clone(),
                date: Timestamp::now(),
                type_: Interet::get_type(type_).to_string(),
                member: if procuration.is_none() {author_member.user.id.get()} else {0},
            });
            let ecrit = bot.database.get(&object_id).unwrap();
            bot.log(&ctx, format!("{} a marqué un intérêt sur l'écrit {} (id: {object_id}) pour {}",
                                  tools::user_desc(ctx.author()),
                                  ecrit.get_name(),
                                  procuration.unwrap_or(ctx.author().display_name().to_string())
            )).await?;
        } else {
            ctx.say(format!("L’écrit « {} » n’est pas ouvert à la critique.", bot.database.get(&object_id).unwrap().get_name())).await?;
        }

    }
    Ok(())
}

/// Libère la marque d’intérêt d’un écrit.
#[poise::command(slash_command, category = "Édition", custom_data = CommandData::perms(Permission::WRITE), check = CommandData::check)]
pub async fn liberer(ctx: Context<'_, DataType, ErrType>,
                     #[description = "Critère d’identification de l’écrit"] critere: String,
                     #[description = "Nom de la personne qui a marqué l’écrit si ce n’est pas la personne exécutant la commande"] procuration: Option<String>) -> Result<(), ErrType> {
    let bot = &mut ctx.data().lock().await;
    if let Some(object_id) = get_object(&ctx, bot, &critere).await? {
        if bot.database.get(&object_id).unwrap().status != Status::OuvertPlus || bot.database.get(&object_id).unwrap().status != Status::Ouvert {
            bot.archive(vec![object_id]);
            let ecrit = bot.database.get_mut(&object_id).unwrap();
            let author_member = ctx.author_member().await.ok_or(ErrType::Generic)?;
            let a_pu_etre_libere =
                if procuration.is_none() {
                    ecrit.liberer_id(author_member.user.id.get())
                } else {
                    ecrit.liberer_name(procuration.as_ref().unwrap())
                };
            if a_pu_etre_libere {
                ctx.say(format!("Écrit « {} » libéré de la marque de {}", ecrit.get_name(),
                                procuration.as_ref().unwrap_or(author_member.nick.as_ref().unwrap_or(&author_member.user.name)))).await?;
                let ecrit = bot.database.get(&object_id).unwrap();
                bot.log(&ctx, format!("{} a libéré un intérêt sur l'écrit {} (id: {object_id}) pour {}",
                                      tools::user_desc(ctx.author()),
                                      ecrit.get_name(),
                                      procuration.unwrap_or(ctx.author().display_name().to_string())
                )).await?;
            } else {
                ctx.say(format!("Aucune marque d’intérêt de {} pour l’écrit « {} ».",
                                procuration.as_ref().unwrap_or(author_member.nick.as_ref().unwrap_or(&author_member.user.name)),
                                ecrit.get_name())).await?;
            }
        } else {
            ctx.say(format!("L’écrit « {} » n’est pas ouvert à la critique.", bot.database.get(&object_id).unwrap().get_name())).await?;
        }

    }
    Ok(())
}

/// Indique qu’un écrit a été critiqué et qu’il est désormais en attente.
#[poise::command(slash_command, category = "Édition", custom_data = CommandData::perms(Permission::WRITE), check = CommandData::check)]
pub async fn critique(ctx: Context<'_, DataType, ErrType>,
                     #[description = "Critère d’identification de l’écrit"] critere: String) -> Result<(), ErrType> {
    let bot = &mut ctx.data().lock().await;
    if let Some(object_id) = get_object(&ctx, bot, &critere).await? {
        if bot.database.get(&object_id).unwrap().status == Status::Ouvert || bot.database.get(&object_id).unwrap().status == Status::OuvertPlus {
            bot.archive(vec![object_id]);
            let ecrit = bot.database.get_mut(&object_id).unwrap();
            ecrit.status = Status::EnAttente;
            ecrit.modified = true;
            ctx.say(format!("Écrit « {} » critiqué !", ecrit.get_name())).await?;
            let ecrit = bot.database.get(&object_id).unwrap();
            bot.log(&ctx, format!("{} a marqué l'écrit {} (id: {object_id}) comme critiqué.", tools::user_desc(ctx.author()), ecrit.get_name())).await?;
        } else {
            ctx.say(format!("L’écrit « {} » n’est pas ouvert à la critique.", bot.database.get(&object_id).unwrap().get_name())).await?;
        }
    }
    Ok(())
}

/// Marque « Sans Nouvelles » tous les écrits antérieurs à la date donnée.
#[poise::command(slash_command, category = "Édition", custom_data = CommandData::perms(Permission::MANAGE), check = CommandData::check)]
pub async fn archiver_avant(ctx: Context<'_, DataType, ErrType>,
    #[description = "Date au format jj/mm/aaaa"] date: String) -> Result<(), ErrType> {
    ctx.defer().await?;
    let bot = &mut ctx.data().lock().await;
    if let Some(date) = parse_date(date) {
        let to_mark: Vec<u64> = bot.database.iter_mut()
            .filter(| (_, ecrit) | {
                (ecrit.last_update < date) && (ecrit.status == Status::Ouvert || ecrit.status == Status::OuvertPlus)
            })
            .map(|(&id, _)| id).collect();
        bot.archive(to_mark.clone());
        let count = to_mark.len();
        to_mark.into_iter().for_each(
            | id | {
                let ecrit = bot.database.get_mut(&id).unwrap();
                (ecrit.status, ecrit.modified) = (Status::SansNouvelles, true)
            }
        );
        ctx.say(format!("{count} écrit(s) ont été marqué(s) sans nouvelles depuis le {}.", date.format("%d %B %Y"))).await?;
        bot.log(&ctx, format!("{} a marqué {count} écrits comme sans nouvelles.", tools::user_desc(ctx.author()))).await?;
    } else {
        ctx.say("Date mal formatée. La date doit être au format jj/nn/aaaa.").await?;
    }

    Ok(())
}

/// Change l’auteur d’un écrit.
#[poise::command(slash_command, category = "Édition", custom_data = CommandData::perms(Permission::WRITE), check = CommandData::check)]
pub async fn auteur(ctx: Context<'_, DataType, ErrType>,
                            #[description = "Critère d’identification de l’écrit"] critere: String,
                            #[description = "Nouvel auteur"] auteur: String ) -> Result<(), ErrType> {
    let bot = &mut ctx.data().lock().await;
    if let Some(object_id) = get_object(&ctx, bot, &critere).await? {
        bot.archive(vec![object_id]);
        let ecrit = bot.database.get(&object_id).unwrap();
        ctx.say(format!("L’auteur de l’écrit « {} » changé pour « {auteur} »", ecrit.get_name())).await?;
        bot.log(&ctx, format!("{} a changé l'auteur de {} (id: {object_id}) de {} à {auteur}",
            tools::user_desc(ctx.author()),
            ecrit.get_name(),
            ecrit.auteur
        )).await?;
        let ecrit = bot.database.get_mut(&object_id).unwrap();
        ecrit.auteur = auteur;
        ecrit.modified = true;
    }
    Ok(())
}

/// Liste les écrits avec des critères précis.
#[poise::command(slash_command, category = "Recherche", custom_data = CommandData::perms(Permission::READ), check = CommandData::check)]
pub async fn ulister(ctx: Context<'_, DataType, ErrType>,
                    #[description = "Inclus dans le nom de l’écrit"] nom: Option<String>,
                    #[description = "Auteurs, séparés par des virgules"] auteurs: Option<String>,
                    #[description = "Statuts, séparés par des virgules"] statuts: Option<String>,
                    #[description = "Types, séparés par des virgules"] types: Option<String>,
                    #[description = "Tags, séparés par des virgules"] tags: Option<String>,
                    #[description = "Si Vrai, les écrits doivent posséder tous les tags donnés. Sinon, un seul suffit (défaut)."] tags_et: Option<bool>,
                    #[description = "Date maximale de dernière modification de l’écrit (jj/mm/aaaa)"] modifie_avant: Option<String>,
                    #[description = "Date minimale de dernière modification de l’écrit (jj/mm/aaaa)"] modifie_apres: Option<String>) -> Result<(), ErrType> {
    ctx.defer().await?;
    let bot = &mut ctx.data().lock().await;

    if nom.is_none() && auteurs.is_none() && statuts.is_none() && types.is_none() && tags.is_none() && modifie_avant.is_none() && modifie_apres.is_none() {
        ctx.say("Il faut au moins un paramètre non-nul.").await?;
        return Ok(())
    }

    /* Traitement des paramètres */
    let nom = basicize(nom.unwrap_or(String::new()).as_str());
    let mut errs = Vec::new();
    let statuts = statuts.and_then(|s| {Some(s.split(",").map(Status::from_str)
        .filter_map(| e | {
            match e {
                Ok(s) => Some(s),
                Err(e) => {errs.push(e); None}
            }
        }).collect())}).unwrap_or(Vec::new());

    let types = types.and_then(|s| {Some(s.split(",").map(Type::from_str)
        .filter_map(| e | {
            match e {
                Ok(s) => Some(s),
                Err(e) => {errs.push(e); None}
            }
        }).collect())}).unwrap_or(Vec::new());

    if !errs.is_empty() {
        return Err(errs.pop().unwrap())
    }

    let auteurs = auteurs.and_then(|s| {Some(s.split(",").map(basicize).map(
        |auteur_critere| {
            let auteurs_vec = Ecrit::recherche_auteur(&auteur_critere, &bot.database);
            if auteurs_vec.is_empty() {
                Err(format!("Aucun auteur correspondant au critère {auteur_critere} trouvé dans la base de données."))
            } else if auteurs_vec.len() > 1 {
                Err(format!("Plus d’un auteur de la base de donnée correspond au critère {auteur_critere}."))
            } else {
                Ok(auteurs_vec[0])
            }
        }
    ).collect())}).unwrap_or(Vec::new());

    let auteurs_errors: Vec<&String> = auteurs.iter().filter_map(|res| match res {
        Err(e) => Some(e),
        _ => None
    }).collect();

    if !auteurs_errors.is_empty() {
        ctx.say(auteurs_errors.into_iter().fold(String::new(), |s, err|
            s + err.as_str() + "\n"
        )).await?;
        return Ok(())
    }

    let auteurs: Vec<&String> = auteurs.into_iter().map(|res| res.unwrap()).collect();

    let tags: Vec<String> = tags.and_then(|s| {Some(s.split(",").map(basicize).collect())}).unwrap_or(Vec::new());
    let modifie_avant = modifie_avant.and_then(parse_date);
    let modifie_apres = modifie_apres.and_then(parse_date);

    let res = tools::sort_by_date(Ecrit::ulister(bot, nom, statuts, types, auteurs, tags, tags_et.unwrap_or(true), modifie_avant, modifie_apres)
        .into_iter().map(|id| {(id, bot.database.get(id).unwrap())}).collect());

    if res.is_empty() {
        ctx.send(CreateReply::default().embed(CreateEmbed::new()
            .title("Aucun résultat.")
            .color(16001600)
            .author(CreateEmbedAuthor::new(String::from("Recherche personnalisée")))
            .timestamp(Timestamp::now()))).await?;
    } else {
        let embeds = tools::get_multimessages(
            tools::create_paged_list(res, |(_, ecrit)| ecrit.get_list_entry(), 1000),
            CreateEmbed::new()
                .author(CreateEmbedAuthor::new(String::from("Recherche personnalisée")))
                .title("Résultats de la recherche")
                .color(73887)
                .timestamp(Timestamp::now())
        );
        bot.send_embed(&ctx, embeds).await?;
    }
    Ok(())
}

/// Ajoute un tag à l’écrit sélectionné.
#[poise::command(slash_command, custom_data = CommandData::perms(Permission::WRITE), check = CommandData::check)]
pub async fn atag(ctx: Context<'_, DataType, ErrType>,
                    #[description = "Critère d’identification de l’écrit"] critere: String,
                    #[description = "Tag à ajouter"] tag: String ) -> Result<(), ErrType> {
    let bot = &mut ctx.data().lock().await;
    if let Some(object_id) = get_object(&ctx, bot, &critere).await? {
        let ecrit = bot.database.get(&object_id).unwrap();
        if ecrit.tags.contains(&tag) {
            ctx.say(format!("Le tag « {tag} » est déjà appliqué à l’écrit « {} ».", ecrit.nom))
        } else {
            bot.archive(vec![object_id]);
            let ecrit = bot.database.get_mut(&object_id).unwrap();
            ecrit.tags.push(tag.clone());
            ecrit.modified = true;
            let ecrit = bot.database.get(&object_id).unwrap();
            bot.log(&ctx, format!("{} a ajouté le tag {tag} à l'écrit {} (id: {object_id}).", tools::user_desc(ctx.author()), ecrit.get_name())).await?;
            ctx.say(format!("Le tag « {tag} » a été ajouté à l’écrit « {} » !", ecrit.nom))
        }.await?;
    }
    Ok(())
}

/// Retire des tags à l’écrit sélectionné.
#[poise::command(slash_command, custom_data = CommandData::perms(Permission::WRITE), check = CommandData::check)]
pub async fn rtag(ctx: Context<'_, DataType, ErrType>,
                  #[description = "Critère d’identification de l’écrit"] critere: String,
                  #[description = "Critère d’identification des tags"] critere_tag: String ) -> Result<(), ErrType> {
    let bot = &mut ctx.data().lock().await;
    if let Some(object_id) = get_object(&ctx, bot, &critere).await? {
        let ecrit = bot.database.get(&object_id).unwrap();
        let critere_tag = basicize(critere_tag.as_str());
        let critere_tag = critere_tag.split(" ");
        let tags_to_keep: Vec<String> = ecrit.tags.iter().filter( | tag |
            critere_tag.clone().all(|mot_critere|
                basicize(tag).split(" ").any(| mot_tag | mot_tag.contains(mot_critere))
            )
        ).map(| tag | tag.clone()).collect();
        if tags_to_keep.len() < ecrit.tags.len() {
            bot.archive(vec![object_id]);
            let ecrit = bot.database.get_mut(&object_id).unwrap();
            ecrit.tags = tags_to_keep;
            ecrit.modified = true;
            let ecrit = bot.database.get(&object_id).unwrap();
            bot.log(&ctx, format!("{} a retiré les tags correspondant au critère {critere} à l'écrit {} (id: {object_id}).", tools::user_desc(ctx.author()), ecrit.get_name())).await?;
            ctx.say(format!("Les tags correspondant au critère ont été retirés de l’écrit « {} » !", ecrit.get_name()))
        } else {
            ctx.say(format!("Aucun tag correspondant trouvé pour l’écrit « {} ».", ecrit.get_name()))
        }.await?;
    }
    Ok(())
}

/// Liste les différents tags existants dans la base de données.
#[poise::command(slash_command, custom_data = CommandData::perms(Permission::READ), check = CommandData::check)]
pub async fn lister_tags(ctx: Context<'_, DataType, ErrType>) -> Result<(), ErrType> {
    ctx.defer().await?;
    let bot = &mut ctx.data().lock().await;
    let tags_total = bot.database.iter().fold(HashMap::new(),
      |container, (_, ecrit)| {
          let mut new_container = container;
          ecrit.tags.iter().for_each(
              |tag| {
                  match new_container.get(tag) {
                      Some(&count) => { new_container.insert(tag.clone(), count + 1); }
                      None => { new_container.insert(tag.clone(), 1); }
                  }
              }
          );
          new_container
      }
    );

    if tags_total.is_empty() {
        ctx.send(CreateReply::default().embed(CreateEmbed::new()
            .title("Aucun tag dans la base de données.")
            .color(16001600)
            .author(CreateEmbedAuthor::new("Liste des tags"))
            .timestamp(Timestamp::now()))).await?;
    } else {
        let messages = tools::create_paged_list(tags_total.into_iter().collect(),
                                                |(tag, nb_ecrits)| format!("**{tag}**\n{nb_ecrits} écrit(s)\n\n"), 1000);
        bot.send_embed(&ctx, tools::get_multimessages(messages, CreateEmbed::new()
            .title("Liste des tags").author(CreateEmbedAuthor::new("Liste des tags"))
            .timestamp(Timestamp::now()).color(73887))).await?;
    }

    Ok(())
}

/// Renvoie un écrit ouvert aléatoire du type demandé.
#[poise::command(slash_command, custom_data = CommandData::perms(Permission::READ), check = CommandData::check)]
pub async fn aleatoire(ctx: Context<'_, DataType, ErrType>,
    #[description = "Type demandé, tous types si non spécifié"]
    #[rename = "type"] type_: Option<Type>) -> Result<(), ErrType> {
    let bot = &mut ctx.data().lock().await;
    let candidats = Ecrit::ulister(bot, "".to_string(), vec![Status::Ouvert],
                                   type_.and_then(|type_| {Some(vec![type_])}).unwrap_or(Vec::new()),
                                    Vec::new(), Vec::new(), false, None, None);
    #[allow(unused_assignments)] /* Necessary to ensure rand falls out of scope before calling the await */
    let mut chosen = None;
    {
        let mut rand = thread_rng();
        chosen = candidats.into_iter().choose(&mut rand);
    }
    if let Some(ecrit_id) = chosen {
        ctx.send(CreateReply::default().embed(bot.database.get(ecrit_id).unwrap().get_embed())
            .components(vec![bot.database.get(ecrit_id).unwrap().get_buttons()])).await?;
    } else {
        ctx.say("Aucun écrit sélectionnable dans la base de données.").await?;
    }
    Ok(())
}

/// Renvoie le plus ancien écrit ouvert du type demandé.
#[poise::command(slash_command, custom_data = CommandData::perms(Permission::READ), check = CommandData::check)]
pub async fn ancien(ctx: Context<'_, DataType, ErrType>,
                    #[description = "Type demandé, tous types si non spécifié"]
                    #[rename = "type"] type_: Option<Type>) -> Result<(), ErrType> {
    let bot = &mut ctx.data().lock().await;
    let candidats = Ecrit::ulister(bot, "".to_string(), vec![Status::Ouvert],
                                   type_.and_then(|type_| {Some(vec![type_])}).unwrap_or(Vec::new()),
                                   Vec::new(), Vec::new(), false, None, None);
    if !candidats.is_empty() {
        let first = *candidats.first().unwrap();
        let oldest = candidats.into_iter().fold(first,
           | oldest, candidat | {
            if bot.database.get(candidat).unwrap().last_update < bot.database.get(oldest).unwrap().last_update {
                candidat
            } else {
                oldest
            }
       });
        ctx.send(CreateReply::default().embed(bot.database.get(oldest).unwrap().get_embed())
            .components(vec![bot.database.get(oldest).unwrap().get_buttons()])).await?;
    } else {
        ctx.say("Aucun écrit sélectionnable dans la base de données.").await?;
    }
    Ok(())
}

/// Affiche la page d’aide du bot.
#[poise::command(slash_command, prefix_command, custom_data = CommandData::perms(Permission::READ), check = CommandData::check)]
pub async fn aide(ctx: Context<'_, DataType, ErrType>) -> Result<(), ErrType> {
    ctx.send(CreateReply::default().embed(CreateEmbed::new()
        .title("Aide de Critibot")
        .description("Les paramètres entre crochets sont optionnels, entre accolades obligatoires. La description des options est disponible en description des commandes slash.")
        .fields(vec![
            ("Commandes de base",
             "`/aide` : Cette commande d'aide.\n\
            `/annuler` : Annule la dernière modification effectuée.", false),
            ("Commandes de gestion et d'affichage de la liste",
            "`/ajouter {Nom} {Auteur} {Type} {Statut} {URL}` : Ajoute manuellement un écrit à la liste.\n\
            `/supprimer {Critère}` : Supprime un écrit. Le Critère doit être assez fin pour aboutir à un unique écrit. __**ATTENTION**__ : Il n'y a pas de confirmation, faites attention à ne pas vous tromper dans le Critère.\n", false),
            ("Commandes de recherche",
            "`/rechercher {Critère}` : Affiche tous les écrits contenant {Critère}.\n\
            `/lister {Statut} [Type]` : Affiche la liste des écrits avec le statut et du type demandés.\n\
            `/lister_tags` : Affiche tous les tags existants dans la base de données et le nombre d'écrits y étant associés.", false),
            ("Commandes de critique",
            "`/marquer {Critère} [Procuration]` : Ajoute une marque d'intérêt à un écrit. Le Critère doit être assez fin pour aboutir à un unique écrit.\n\
            `/libérer {Critère} [Procuration]` : Supprime une marque d'intétêt sur un écrit. Le Critère doit être assez fin pour aboutir à un unique écrit.\n\
            `/up {Critère}` : Marque un écrit ouvert et le remet au premier plan dans le salon des fils ouverts s'il l'était déjà. Le Critère doit être assez fin pour aboutir à un unique écrit.\n\
            `/valider {Critère}` : Change le type du rapport en Rapport et le marque En Attente si c'était une idée, règle le statut à Validé sinon. Le Critère doit être assez fin pour aboutir à un unique écrit.", false),
            ("Commandes d'entretien de la base de données (À utiliser avec précaution)",
            "`/nettoyer` : Supprime tous les écrits abandonnés / refusés / publiés de la liste.\n\
            `/archiver_avant {Date}` : Met le statut « sans nouvelles » à tous les écrits n'ayant pas été mis à jour avant la date indiquée. La date doit être au format dd/mm/yyyy.\n\
            `/doublons` : Supprime les éventuels doublons.", false),
            ("Commandes de choix d’écrit",
            "`/aléatoire [Type]` : Choisit un écrit ouvert aléatoire du type donné en paramètre. Si aucun argument n'est donné, chosit un écrit ouvert aléatoire sans distinction de type.\n\
            `/ancien [Type]` : Choisit l'écrit le plus anciennement modifié encore ouvert du type donné en paramètre. Si aucun argument n'est donné, choisit l'écrit encore ouvert le plus ancien sans distinction de type.", false),
            ("Recherche avancée",
            "La recherche avancée est utilisable avec `/ulister`. En mode texte, chaque paramètre est de la forme `nom=valeur`. Les différents paramètres disponibles sont :\n\
            `nom: {Critère}` : Réduit la recherche aux écrits dont le nom correspond au critère.\n\
            `statut: {Statut},{Statut},…` : Les écrits doivent avoir l'un des statuts de la liste.\n\
            `type: {Type},{Type},…` : Les écrits doivent avoir l'un des types de la liste.\n\
            `auteur: {Critère auteur},{Critère auteur},…` : Les écrits doivent être d'un des auteurs de la liste.\n\
            `tag: {Critère tag},{Critère tag},…` : Les écrits doivent posséder l'un des tags de la liste.\n\
            `tag_et` : Si \"Vrai\", l’écrit doit posséder tous les tags trouvés plutôt qu’un seul.\n\
            `avant: {jj/mm/aaaa}` : Les écrits doivent avoir été modifiés pour la dernière fois avant la date indiquée.\n\
            `après: {jj/mm/aaaa}` : Les écirts doivent avoir été modifiés pour la dernière fois après la date indiquée.", false),
            ("Code source", "Disponible sur [Github](https://github.com/Fondation-SCP/critibot).", false)
        ])
        .footer(CreateEmbedFooter::new("Version 4.1.0 (Rust 1.1.0)"))
        .author(CreateEmbedAuthor::new("Critibot").icon_url("https://media.discordapp.net/attachments/719194758093733988/842082066589679676/Critiqueurs5.jpg"))
    )).await?;
    Ok(())
}


pub fn command_list() -> Vec<Command<DataType, ErrType>> {
    vec![ajouter(), lister(), nettoyer(), statut(), type_(), marquer(), liberer(), critique(),
         archiver_avant(), auteur(), ulister(), atag(), rtag(), lister_tags(), alias("ajouter_tag", atag()),
        alias("retirer_tag", rtag()), alias("supprimer_tag", rtag()), aleatoire(), alias("random", aleatoire()),
        ancien(), aide(), alias("help", aide()), valider()]
}
